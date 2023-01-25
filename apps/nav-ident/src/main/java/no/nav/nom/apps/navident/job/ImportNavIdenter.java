package no.nav.nom.apps.navident.job;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.navident.domain.IdentType;
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
import java.io.Reader;
import java.nio.channels.Channels;
import java.time.Duration;
import java.time.LocalDate;

@Service
@Slf4j
public class ImportNavIdenter {

    @Value("${app.gs-bucket-name}")
    private String bucketName;
    @Value("${nais.cluster.name:local}")
    private String gsProjectId;
    @Value("${app.kafka.topic.eventsource.name}")
    private String eventSourceTopic;
    private final SourceEventRepository sourceEventRepository;
    private final EventSourceService eventSourceService;

    private final KafkaTemplate<String, SourceEvent> producer;


    public ImportNavIdenter(SourceEventRepository sourceEventRepository, EventSourceService eventSourceService, KafkaTemplate<String, SourceEvent> producer) {
        this.sourceEventRepository = sourceEventRepository;
        this.eventSourceService = eventSourceService;
        this.producer = producer;
    }

    public String fromBucketObject(String objectName) {
        log.info("ImportNavIdenter: {} - {}/{}", gsProjectId, bucketName, objectName);
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            log.info("Blob in bucket {}: {}", bucketName, blob.getName());
        }

        StringBuffer sb = new StringBuffer();
        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, objectName))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, "UTF-8"));
            return importFromReader(br);
        } catch (Exception e) {
            log.error("Unable to read input", e);
            return "Unable to read. " + e.getMessage();
        }
    }

    public String importFromReader(BufferedReader reader) throws IOException {
        LocalDate start = LocalDate.now();
        int total = 0, updated = 0;
        while(reader.ready()) {
            total++;
            if (importTestNavIdent(reader.readLine()) ) updated++;
        }
        String result = "Totalt importert %s av %s - tid brukt: %s".formatted(updated, total, durationAsH24(start));
        log.info(result);
        return result;
    }

    public boolean importTestNavIdent(String importLine) {
        var splitted = importLine.split(",");
        if (splitted.length < 2) {
            log.error("importLine kan ikke splittes på ',': {}", importLine);
            return false;
        }
        var navIdent = splitted[0];
        var personIdent = splitted[1];

        if (navIdent.length() != 7) {
            log.error("Lest navIdent '{}' fra importlinje er ikke 7 tegn: {}", navIdent, importLine);
            return false;
        }
        if (personIdent.length() != 11) {
            log.error("Lest navIdent '{}' fra importlinje er ikke 7 tegn: {}", personIdent, importLine);
            return false;
        }

        if (finnesAllerede(navIdent)) {
            log.error("Lest navIdent '{}' fra importlinje finnes i fra før: {}", navIdent, importLine);
            return false;
        }

        val sourceEvent = SourceEvent.builder()
            .event(EventType.OPPRETT)
            .payload(
                EventPayload.builder()
                    .navident(navIdent)
                    .identType(IdentType.STANDARD)
                    .personid(personIdent)
                    .build()
            ).build();


        val persistedSourceEvent = sourceEventRepository.save(sourceEvent);

        producer.send(eventSourceTopic, persistedSourceEvent);

        log.debug("Persisted SourceEvent posted: %s".formatted(persistedSourceEvent.maskedToString()));
        return true;
    }

    private boolean finnesAllerede(String navIdent) {
        return eventSourceService.finnesNavIdent(navIdent);
    }

    private String durationAsH24(LocalDate start) {
        Duration duration = Duration.between(start, LocalDate.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
