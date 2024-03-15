package no.nav.nom.apps.navident.job;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.apps.navident.eventsource.EventPayload;
import no.nav.nom.apps.navident.eventsource.EventSourceService;
import no.nav.nom.apps.navident.eventsource.EventType;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.eventsource.SourceEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
@Slf4j
public class ImportNavIdenter {

    @Value("${app.gs-bucket-name}")
    private String bucketName;
    @Value("${nais.cluster.name:local}")
    private String gsProjectId;
    @Value("${app.kafka.topic.eventsource.name}")
    private String eventSourceTopic;
    @Value("${app.navident.sequence}")
    private int navidentSequence;

    private final SourceEventRepository sourceEventRepository;
    private final EventSourceService eventSourceService;

    private final KafkaTemplate<String, SourceEvent> producer;

    private Map<String, String> importerteNavIdenter;
    private int skippedLines = 0;

    public ImportNavIdenter(SourceEventRepository sourceEventRepository, EventSourceService eventSourceService, KafkaTemplate<String, SourceEvent> producer) {
        this.sourceEventRepository = sourceEventRepository;
        this.eventSourceService = eventSourceService;
        this.producer = producer;
    }

    public String fromBucketObject(String objectName, boolean kunLes) {
        log.info("ImportNavIdenter: {} - {}/{}", gsProjectId, bucketName, objectName);
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            log.info("Blob in bucket {}: {}", bucketName, blob.getName());
        }

        StringBuffer sb = new StringBuffer();
        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, objectName))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, "UTF-8"));
            return importFromReader(br, kunLes);
        } catch (Exception e) {
            log.error("Unable to read input", e);
            return "Unable to read. " + e.getMessage();
        }
    }

    public String importFromReader(BufferedReader reader, boolean kunLes) throws IOException {
        importerteNavIdenter = new HashMap<>();
        LocalDateTime start = LocalDateTime.now();
        int total = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            total++;
            importTestNavIdent(line, kunLes);
        }
        String result = "Totalt importert %s av %s (inkludert header). Skipped = %s - tid brukt: %s".formatted(importerteNavIdenter.size(), total, skippedLines, durationAsH24(start));
        log.info(result);
        return result;
    }

    // NAV-ID,Person id,Bruker id,Fornavn,Etternavn,Enhets-ID,Enhet,Status,Sektor,I permisjon?,I permisjon dato,Reg. dato,Tiltredelsesdato,Status-History.Slettet.TIME
    public void importTestNavIdent(String importLine, boolean kunLes) {
        log.debug("line: {}", importLine);
        var kolonner = importLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Split på komma, ignorer komma i "
        if (kolonner.length < 3) {
            log.error(LogConfig.SECURE_LOGS, "importLine kan ikke splittes på ',': {}", importLine);
            return;
        }
        var navIdent = kolonner[0];
        var personIdent = kolonner[4];
        var status = kolonner[7];
        var sektor = kolonner[8];

        if (!status.equals("Aktiv")) {
            if (!status.equals("Slettet")) {
                log.info(LogConfig.SECURE_LOGS, "Skipping - men status != Slettet. => {}: {}", status, importLine);
            }
            log.debug("Skipping rad uten status Aktiv");
            skippedLines++;
            return;
        }
        if (navIdent.length() != 7) {
            log.error(LogConfig.SECURE_LOGS, "Lest navIdent '{}' fra importlinje er ikke 7 tegn: {}", navIdent, importLine);
            return;
        }
        if (finnesAllerede(navIdent)) {
            log.error(LogConfig.SECURE_LOGS, "Lest navIdent '{}' fra importlinje finnes i fra før: {}", navIdent, importLine);
            return;
        }

        if (personIdent.length() == 0) {
            log.error(LogConfig.SECURE_LOGS, "Lest personIdent fra importlinje er tom", personIdent, importLine);
            return;
        }

        if (importerteNavIdenter.get(navIdent) != null) {
            if (importerteNavIdenter.get(navIdent).equals(personIdent)) {
                log.info(LogConfig.SECURE_LOGS,"Navident '{}' allerede importert på samme personid", navIdent);
            } else {
                log.error(LogConfig.SECURE_LOGS, "Navident '{}' allerede importert, men på en annen personid ({}): {}", navIdent, importerteNavIdenter.get(navIdent), importLine);
            }
            return;
        }

        importerteNavIdenter.put(navIdent, personIdent);

        val sourceEvent = SourceEvent.builder()
            .event(EventType.OPPRETT)
            .payload(
                EventPayload.builder()
                    .navident(navIdent)
                    .identType(parse(sektor))
                    .personid(personIdent)
                    .build()
            ).build();

        if (!kunLes) {
            sourceEventRepository.save(sourceEvent);
            producer.send(eventSourceTopic, sourceEvent);

            log.debug(LogConfig.SECURE_LOGS, "Persisted SourceEvent posted: %s".formatted(sourceEvent.maskedToString()));
        }
    }


    /**
     * Importer ident type fra en ressursfil NIK_identType.txt som inneholder en liste over identer med riktig typer med navident_seq høyere enn 168000.
     */
    public String importIdentTypeFromResourceFile(final InputStream inputStream) {
        LocalDateTime start = LocalDateTime.now();
        Map<String, String> identsReadFromFile = readIdentTypeFromResourceFile(inputStream);
        int totalIdentUpdated = sendEndreEvents(identsReadFromFile);
        String result = "Totalt ident importert fra fil %s av %s. Tid brukt: %s".formatted(totalIdentUpdated, identsReadFromFile.size(), durationAsH24(start));
        log.info(result);
        return result;
    }

    /*
    public Map<String, String> readIdentTypeFromResourceFile(final Resource resource) {
        Map<String, String> identImportedFromFile = new HashMap<>();
        try {
            File file = resource.getFile();
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] kolonner = line.split(",");
                if (isValidNavIdentAndHasValidSequenceNumber(kolonner[0])){
                    identImportedFromFile.put(kolonner[0], kolonner[1]);
                }
            }
        } catch (Exception e) {
            log.error("Feil ved import av ident type fra ressursfil", e);
        }
        return identImportedFromFile;
    }
    */

    public Map<String, String> readIdentTypeFromResourceFile(final InputStream inputStream) {
        Map<String, String> identImportedFromFile = new HashMap<>();
        try {
            Scanner sc = new Scanner(inputStream);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] kolonner = line.split(",");
                if (isValidNavIdentAndHasValidSequenceNumber(kolonner[0])){
                    identImportedFromFile.put(kolonner[0], kolonner[1]);
                }
            }
        } catch (Exception e) {
            log.error("Feil ved import av ident type fra ressursfil", e);
        }
        return identImportedFromFile;
    }

    public int sendEndreEvents(Map<String, String> navIdents) {
        int totalIdentUpdated = 0;
        for (Map.Entry<String, String> entry : navIdents.entrySet()) {
            if(eventSourceService.finnesNavIdent(entry.getKey())){
                Navident navIdent = eventSourceService.getsNavIdent(entry.getKey());
                if (!navIdent.getIdentType().name().equals(entry.getValue())) {
                    sendEndreEvent(navIdent, entry.getValue());
                    totalIdentUpdated++;
                }
            }
        }
        return totalIdentUpdated;
    }

    private void sendEndreEvent(Navident navIdent, String identType) {
        SourceEvent sourceEvent = SourceEvent.builder()
                .event(EventType.ENDRE)
                .payload(
                        EventPayload.builder()
                                .navident(navIdent.getNavident())
                                .personid(navIdent.getGjeldendePersonid())
                                .identType(IdentType.valueOf(identType))
                                .build()
                ).build();
        sourceEventRepository.save(sourceEvent);
        producer.send(eventSourceTopic, sourceEvent);
        log.debug(LogConfig.SECURE_LOGS, "Persisted SourceEvent posted: %s".formatted(sourceEvent.maskedToString()));
    }

    public boolean isValidNavIdentAndHasValidSequenceNumber(String navIdent) {
        return navIdent.length() == 7 && Integer.parseInt(navIdent.substring(1)) < navidentSequence;
    }


    private IdentType parse(String identType) {
        switch (identType) {
            case "NAV Statlig":
            case "NAV Kommunalt":
            case "Eksternt":
                return IdentType.STANDARD;
            default:
                throw new RuntimeException("Ukjent ident type %s".formatted(identType));
        }
    }

    private boolean finnesAllerede(String navIdent) {
        return eventSourceService.finnesNavIdent(navIdent);
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
