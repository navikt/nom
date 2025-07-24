package no.nav.nom.apps.api.orgtilknytning.job;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;

@Service
@Slf4j
public class ImportMissingOrgtilknytninger {

    @Value("${app.google-storage-bucket-name.missing-ot}")
    private String bucketName;

    @Value("${nais.cluster.name:local}")
    private String gsProjectId;

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final NomMastered nomMastered;


    public ImportMissingOrgtilknytninger(
            OrgtilknytningStateRepository orgtilknytningStateRepository,
            RessursStateRepository ressursStateRepository,
            OrgEnhetStateRepository orgEnhetStateRepository,
            OrgtilknytningCommandService orgtilknytningCommandService,
            NomMastered nomMastered
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.nomMastered = nomMastered;
    }


    public String importfromBucket(String objectName, boolean kunLes) {
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            log.info("Blob i bucket {}: {}", bucketName, blob.getName());
        }

        StringBuilder sb = new StringBuilder("Fid for orgtilknytninger opprettet: ");
        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, objectName))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, StandardCharsets.UTF_8));
            
            List<MissingOrgtilknytning> missingOrgtilknytninger = readMissingOrgtilknytninger(br);

            if (!kunLes) {
                int numberOfOTCreated = 0;
                for (MissingOrgtilknytning missingOt : missingOrgtilknytninger) {
                    var res = createEventsForNewOrgtilknytning(missingOt);
                    if (!res.isEmpty()) {
                        numberOfOTCreated++;
                        sb.append(res).append("\n");
                    }
                }
                log.info("Antall orgTilkyntninger opprettet: {}", numberOfOTCreated);
            }

        } catch (Exception e) {
            log.error("Klarer ikke å lese fra bucket: ", e);
            return "Klarer ikke å lese fra bucket: " + e.getMessage();
        }

        return sb.toString();
    }


    public List<MissingOrgtilknytning> readMissingOrgtilknytninger(BufferedReader reader) throws IOException {

        List<MissingOrgtilknytning> missingOts = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (!isHeaderLine(line)) {
                parseAndValidateLine(line).ifPresent(missingOts::add);
            }
        }
        log.info("Leste {} manglende orgtilknytninger fra bucket {}", missingOts.size(), bucketName);

        return missingOts;
    }


    // NAV-ident,Navn,Sektor,Enhetsnummer,Orgenhet Unit4 nr,Orgenhet Unit4 navn,NOM_orgenhet,NOM_orgnavn,gyldig_tom,Kommentar
    private Optional<MissingOrgtilknytning> parseAndValidateLine(String line) {
        log.debug(TEAM_LOGS, "Parsing line: {}", line);
        var kolonner = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Split på komma, ignorer komma i "

        MissingOrgtilknytning missingOt = new MissingOrgtilknytning(kolonner[0], kolonner[1], kolonner[6], kolonner[7]);

        if (missingOt.navident().isEmpty()) {
            log.warn(TEAM_LOGS, "Navident '{}' er tom. Importlinje: {}", missingOt.navident(), line);
            return Optional.empty();
        }

        if (missingOt.navident().length() != 7 || !missingOt.navident().matches("[A-Z]\\d+")) {
            log.warn(TEAM_LOGS, "Navident '{}' er ikke gyldig. Importlinje: {}", missingOt.navident(), line);
            return Optional.empty();
        }

        if (missingOt.orgId().isEmpty()) {
            log.warn(TEAM_LOGS, "Orgenhet ID '{}' er tom. Importlinje: {}", missingOt.orgId(), line);
            return Optional.empty();
        }

        if (missingOt.orgId().length() != 6 || !missingOt.orgId().matches("^[a-z]{2}\\d{3}[a-z]$")) {
            log.warn(TEAM_LOGS, "Orgenhet ID '{}' er ikke gyldig. Importlinje: {}", missingOt.orgId(), line);
            return Optional.empty();
        }

        return Optional.of(missingOt);
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("NAV-ident,Navn");
    }

    public LocalDate getFomDato(LocalDate orgEnhetGyldigFom, LocalDate ressursStartDato) {
        if (isNull(ressursStartDato)) {
            return orgEnhetGyldigFom;
        } else {
            if (orgEnhetGyldigFom.isAfter(ressursStartDato)) {
                return orgEnhetGyldigFom;
            } else {
                return ressursStartDato;
            }
        }
    }

    public String createEventsForNewOrgtilknytning(MissingOrgtilknytning missingOt) {

        // Beregne fomDato 
        RessursState ressurs;
        OrgEnhetState orgEnhet;

        var ressursOptional = ressursStateRepository.findByNavident(missingOt.navident());
        if (ressursOptional.isPresent()) {
            ressurs = ressursOptional.get();
        } else {
            log.warn(TEAM_LOGS, "Ressurs med navident {} ({}) ikke funnet", missingOt.navident(), missingOt.navn());
            return "";
        }

        var orgEnhetOptional = orgEnhetStateRepository.findById(missingOt.orgId());
        if (orgEnhetOptional.isPresent()) {
            orgEnhet = orgEnhetOptional.get();
        } else {
            log.warn(TEAM_LOGS, "OrgEnhet med ID {} ({}) ikke funnet", missingOt.orgId(), missingOt.orgNavn());
            return "";
        }

        LocalDate fomDato = getFomDato(orgEnhet.gyldigFom(), ressurs.startdato());
        
        if (ressurs.sluttdato() != null && ressurs.sluttdato().isBefore(LocalDate.now())) {
            log.info(TEAM_LOGS, "Sluttdato {} er passert for ressurs {}, så oppretter ikke orgtilknytning", ressurs.sluttdato(), ressurs.navident());
            return "";
        }

        var eksisterendeOrgtilknytninger = orgtilknytningStateRepository.findByNavident(missingOt.navident());
        var notExpiredOrgtilknytninger = eksisterendeOrgtilknytninger.stream()
                .filter(ot -> !otIsExpired(ot))
                .toList();
        
        if (!notExpiredOrgtilknytninger.isEmpty()) {
            log.warn(TEAM_LOGS, "Ressurs med navident {} har eksisterende orgtilknytninger: {}", missingOt.navident(),
                    eksisterendeOrgtilknytninger);
            return "";
        }

        InputMetadata inputMetadata = new InputMetadata(
                "nom-api manual import job",
                "createEventsForNewOrgtilknytning",
                missingOt,
                new Origination("nom-api", "nom-api")
        );

        var opprettetOrgtilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
                OpprettOrgtilknytning.builder()
                        .navident(missingOt.navident())
                        .orgenhetId(missingOt.orgId())
                        .gyldigFom(fomDato)
                        .inputMetadata(inputMetadata)
                        .additionalAgressoData(null)
                        .build()
        );

        // Må sette daglig oppfølging i egen event
        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
                EndreDagligOppfolgingOrgtilknytningCommand.builder()
                        .fid(opprettetOrgtilknytningFid)
                        .navident(missingOt.navident())
                        .orgenhetId(missingOt.orgId())
                        .gyldigFom(fomDato)
                        .gyldigTom(Optional.ofNullable(ressurs.sluttdato()))
                        .erDagligOppfolging(true)
                        .inputMetadata(inputMetadata)
                        .additionalAgressoData(null)
                        .build()
        );

        // Alle OTer som importeres er for kommunale, så de skal ha tilknytningstype INGEN
        orgtilknytningCommandService.endreTilknytningtype(
                EndreTilknytningtypeCommand.builder()
                        .fid(opprettetOrgtilknytningFid)
                        .tilknytningtype(Optional.of(Tilknytningtype.INGEN))
                        .inputMetadata(inputMetadata)
                        .build()
        );


        boolean masteringOk = nomMastered.markRessursAsMasteredInNom(missingOt.navident());
        if (!masteringOk) {
            log.warn(TEAM_LOGS, "Fikk ikke merket ressurs med navident {} som mastret i NOM", missingOt.navident());
            return "";
        }

        return opprettetOrgtilknytningFid;
    }
    
    private boolean otIsExpired(OrgtilknytningState ot) {
        return ot.gyldigTom().isPresent() && ot.gyldigTom().get().isBefore(LocalDate.now());
    }

    public record MissingOrgtilknytning(
            String navident,
            String navn,
            String orgId,
            String orgNavn
    ) {
    }
}

