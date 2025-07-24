package no.nav.nom.apps.api.ressurs.job;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;

@Slf4j
@Service
public class MissingEksternSluttdatoer {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Value("${app.google-storage-bucket-name.missing-ekstern-sluttdato:}")
    private String bucketName;

    @Value("${nais.cluster.name:local}")
    private String gsProjectId;

    private final RessursStateRepository ressursStateRepository;
    private final RessursCommandService ressursCommandService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final OrgtilknytningService orgtilknytningService;
    private final NomMastered nomMastered;

    public MissingEksternSluttdatoer(
        RessursStateRepository ressursStateRepository, RessursCommandService ressursCommandService,
        OrgtilknytningCommandService orgtilknytningCommandService, OrgtilknytningService orgtilknytningService,
        NomMastered nomMastered
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursCommandService = ressursCommandService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.orgtilknytningService = orgtilknytningService;
        this.nomMastered = nomMastered;
    }

    public String importer(String fileName, boolean dryRun) {
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        try (ReadChannel channel = storage.reader(BlobId.of(bucketName, fileName))) {
            BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8));

            return process(read(reader), dryRun);

        } catch (Exception e) {
            return String.format("Lesing fra bucket mislyktes: %s ", e.getMessage());
        }
    }

    public String process(List<RemedyRessurs> remedyRessurser, boolean dryRun) {
        var stats = new StringBuilder("navident;nom mastret;sluttdato satt\n");

        for (RemedyRessurs remedyRessurs : remedyRessurser) {
            var ressurs = ressursStateRepository.findByNavident(remedyRessurs.navident()).orElse(null);

            stats.append(remedyRessurs.navident()).append(";");

            if (ressurs == null) {
                stats.append("-;-\n");
                continue;
            }

            if (!nomMastered.isMastered(remedyRessurs.navident())) {
                stats.append(";");

                if (ressurs.sluttdato() != null) {
                    stats.append(("x"));
                } else if (!dryRun) {
                    avvikleRessurs(ressurs.fid(), remedyRessurs);

                    orgtilknytningService.getByNavIdent(remedyRessurs.navident()).stream()
                        .filter(ot -> ot.gyldigTom().isEmpty())
                        .forEach(ot -> avvikleOrgtilknytning(ot, remedyRessurs));

                    nomMastered.markRessursAsMasteredInNom(remedyRessurs.navident());
                }
                stats.append("\n");
            } else {
                stats.append("x;x\n");
            }
        }

        return stats.toString();
    }

    private void avvikleRessurs(String fid, RemedyRessurs ressurs) {
        var metadata = new InputMetadata(
            "nom-api import missing ekstern sluttdato",
            "avsluttRessurs",
            ressurs,
            new Origination("nom-api", "nom-api")
        );

        log.debug(TEAM_LOGS, "Lager RessursSluttetEvent event for fid {}", fid);
        ressursCommandService.avsluttRessursEvent(new OpphorRessursCommand(fid, ressurs.sluttDato()), metadata);
    }

    private void avvikleOrgtilknytning(OrgtilknytningState orgtilknytning, RemedyRessurs remedyRessurs) {
        var metadata = new InputMetadata(
            "nom-api import missing ektstern sluttdato",
            "avvikleOrgtilknytning",
            remedyRessurs,
            new Origination("nom-api", "nom-api")
        );

        var avvikleOrgtilknytning = new AvvikleOrgtilknytning(
            remedyRessurs.navident(),
            orgtilknytning.orgEnhetId(),
            remedyRessurs.sluttDato(),
            metadata,
            null
        );

        log.debug(TEAM_LOGS, "Lager AvvikleOrgtilknytning event for fid {}:\n {}", orgtilknytning.fid(), avvikleOrgtilknytning);

        orgtilknytningCommandService.avvikleOrgtilknytning(orgtilknytning.fid(), avvikleOrgtilknytning);
    }

    private List<RemedyRessurs> read(BufferedReader reader) throws IOException {
        var ressurser = new ArrayList<RemedyRessurs>();
        var lines = 1;
        var line = reader.readLine(); // header
        log.info("Read header");

        while ((line = reader.readLine()) != null) {
            parse(line).ifPresent(ressurser::add);
            log.info("Read line {}", ++lines);
        }

        return ressurser;
    }

    private Optional<RemedyRessurs> parse(String line) {
        var kolonner = line.split(";", -1);

        if (!kolonner[0].matches("[A-Z]\\d{6}")) {
            return Optional.empty();
        }

        var date = parseDate(kolonner[1]);

        return date.map(localDate -> new RemedyRessurs(kolonner[0], localDate));

    }

    private Optional<LocalDate> parseDate(String date) {
        // skatt og helfo
        if (StringUtils.trimToNull(date) == null) {
            return Optional.of(LocalDate.now().plusYears(1).minusDays(1));
        }

        try {
            return Optional.of(LocalDate.parse(date.substring(0, 10), formatter));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
