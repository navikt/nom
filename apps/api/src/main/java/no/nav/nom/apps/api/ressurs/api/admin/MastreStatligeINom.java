package no.nav.nom.apps.api.ressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;

@Slf4j
@Tag(name = "Admin for å migrering til NOM som master av statlige ressurser")
@RestController
@RequestMapping("/api/admin/mastrestatlige")
@PreAuthorize("hasAuthority('ADMIN')")
public class MastreStatligeINom {
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final RessursCommandService ressursCommandService;
    private final NomMastered nomMastered;

    static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    public MastreStatligeINom(RessursStateRepository ressursStateRepository, OrgtilknytningService orgtilknytningService,
                              RessursCommandService ressursCommandService, NomMastered nomMastered) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.ressursCommandService = ressursCommandService;
        this.nomMastered = nomMastered;
    }

    @Operation(
        description = "List alle statlige ressurser som ikke er mastret i NOM",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @GetMapping(value = "/listStatligeSomIkkeErMastret")
    public ResponseEntity<String> listStatligeSomIkkeErMastret() {
        StringBuffer sb = new StringBuffer();
        sb.append("navident, sektor, startdato, sluttdato, maxOtSluttDato\n");

        getAlleStatligeSomIkkeErMastret().forEach(getRessursRow(sb));

        return ResponseEntity.ok(sb.toString());
    }

    @Operation(
        description = "Marker alle statlige ressurser som mastret i NOM. Alle navidenter i body vil ekskluderes",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PostMapping(value = "/markerStatligeSomMastret")
    public ResponseEntity<String> markerStatligeSomMastret(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @RequestBody List<String> ekskluderNavidenter
    ) {
        return ResponseEntity.ok(markerStatligeSomMastret("CONFIRM".equalsIgnoreCase(confirm), ekskluderNavidenter));
    }


    @Operation(
        description = "Oppdater sluttDato på Ressurs (skript pkt 3) for utvalg Gruppe A",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PostMapping(value = "/oppdaterRessursSluttdatoGrpA")
    public ResponseEntity<String> oppdaterRessursSluttdatoGrpA(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @Parameter(description = "d.d. + 5 uker elns.") @RequestParam LocalDate maxOTDatoEtter,
        @RequestBody List<String> ekskluderNavidenter
    ) {
        boolean commit = "CONFIRM".equalsIgnoreCase(confirm);

        return oppdaterRessursSluttdatoGrpA(maxOTDatoEtter, ekskluderNavidenter, commit, getAuthenticatedNavIdent());
    }

    protected ResponseEntity<String> oppdaterRessursSluttdatoGrpA(LocalDate maxOTDatoEtter, List<String> ekskluderNavidenter, boolean commit, String userNavident) {
        StringBuffer sb = new StringBuffer();
        if (!commit) {
            sb.append("DRY RUN\n");
        }

        var grpAUtvalg = utvalgGruppeA(maxOTDatoEtter);
        var skalOppdateres = ekskluderNavidenter(grpAUtvalg, ekskluderNavidenter);

        sb.append("GruppeA - alle som har aktiv OT etter: ").append(maxOTDatoEtter).append("\n");
        sb.append("Antall navidenter å ekskludere: ").append(ekskluderNavidenter.size()).append("\n");
        sb.append("Antall i Gruppe A utvalg: ").append(grpAUtvalg.size()).append("\n");
        sb.append("Antall å oppdatere: ").append(skalOppdateres.size()).append("\n");

        var inputMetadata = new InputMetadata(
            "nom-api MastreStatligeINom",
            "oppdaterRessursSluttdatoGrpA",
            Map.of("maxOTDateEtter", maxOTDatoEtter, "ekskluderNavidenter", ekskluderNavidenter),
            new Origination(userNavident, "nom-api")
        );
        oppdaterRessursSluttdato(commit, skalOppdateres, inputMetadata);

        sb.append("\nnavident, sektor, startdato, sluttdato, maxOtSluttDato\n");
        skalOppdateres.forEach(getRessursRow(sb));

        log.info(sb.toString());
        return ResponseEntity.ok(sb.toString());
    }

    @Operation(
        description = "Oppdater sluttDato på Ressurs (skript pkt 3) for utvalg Gruppe B",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PostMapping(value = "/oppdaterRessursSluttdatoGrpB")
    public ResponseEntity<String> oppdaterRessursSluttdatoGrpB(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @Parameter(description = "SAMME dato som brukt i Gruppe A utvalget") @RequestParam LocalDate maxOTDatoLikEllerFoer,
        @Parameter(description = "Max OT dato skal være like eller større enn") @RequestParam(defaultValue="2024-12-01") LocalDate maxOTDatoLikEllerEtter,
        @RequestBody List<String> ekskluderNavidenter
    ) {
        boolean commit = "CONFIRM".equalsIgnoreCase(confirm);

        return oppdaterRessursSluttdatoGrpB(maxOTDatoLikEllerFoer, maxOTDatoLikEllerEtter, ekskluderNavidenter, commit, getAuthenticatedNavIdent());
    }

    protected ResponseEntity<String> oppdaterRessursSluttdatoGrpB(LocalDate maxOTDatoLikEllerFoer, LocalDate maxOTDatoLikEllerEtter, List<String> ekskluderNavidenter, boolean commit, String userNavident) {
        StringBuffer sb = new StringBuffer();
        if (!commit) {
            sb.append("DRY RUN\n");
        }

        var grpBUtvalg = utvalgGruppeB(maxOTDatoLikEllerFoer, maxOTDatoLikEllerEtter);
        var skalOppdateres = ekskluderNavidenter(grpBUtvalg, ekskluderNavidenter);

        sb.append("GruppeB - alle som har ressurs.Sluttdato = null, og aktiv OT i perioden (FOM-TOM): ")
            .append(maxOTDatoLikEllerEtter).append(" - ").append(maxOTDatoLikEllerFoer).append("\n");
        sb.append("Antall navidenter å ekskludere: ").append(ekskluderNavidenter.size()).append("\n");
        sb.append("Antall i Gruppe B utvalg: ").append(grpBUtvalg.size()).append("\n");
        sb.append("Antall å oppdatere: ").append(skalOppdateres.size()).append("\n");

        var inputMetadata = new InputMetadata(
            "nom-api MastreStatligeINom",
            "oppdaterRessursSluttdatoGrpB",
            Map.of("maxOTDat0LikEllerFoer", maxOTDatoLikEllerFoer, "maxOTDatoLikEllerEtter", maxOTDatoLikEllerEtter, "ekskluderNavidenter", ekskluderNavidenter),
            new Origination(userNavident, "nom-api")
        );

        oppdaterRessursSluttdato(commit, skalOppdateres, inputMetadata);

        sb.append("\nnavident, sektor, startdato, sluttdato, maxOtSluttDato\n");
        skalOppdateres.forEach(getRessursRow(sb));

        log.info(sb.toString());
        return ResponseEntity.ok(sb.toString());
    }

    @Operation(
        description = "Oppdater sluttDato på Ressurs (skript pkt 3) for utvalg Gruppe C",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PostMapping(value = "/oppdaterRessursSluttdatoGrpC")
    public ResponseEntity<String> oppdaterRessursSluttdatoGrpC(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @Parameter(description = "SAMME dato som brukt i Gruppe A utvalget") @RequestParam LocalDate maxOTDatoLikEllerFoer,
        @Parameter(description = "Ressurs sluttdato skal være etter (2024-12-31)") @RequestParam(defaultValue="2024-12-31") LocalDate ressursSluttDatoEtter,
        @Parameter(description = "Begrens utvalg til de navidenter som leveres i body fremfor å eksludere dem") @RequestParam(defaultValue="false") boolean begrensUtvalgTilNavidentListe,
        @RequestBody List<String> navidentListe
    ) {
        boolean commit = "CONFIRM".equalsIgnoreCase(confirm);

        return oppdaterRessursSluttdatoGrpC(maxOTDatoLikEllerFoer, ressursSluttDatoEtter, navidentListe, begrensUtvalgTilNavidentListe, commit, getAuthenticatedNavIdent());
    }

    protected ResponseEntity<String> oppdaterRessursSluttdatoGrpC(LocalDate maxOTDatoLikEllerFoer, LocalDate ressursSluttDatoEtter, List<String> navidentListe, boolean begrensUtvalgTilNavidentListe, boolean commit, String userNavident) {
        StringBuffer sb = new StringBuffer();
        if (!commit) {
            sb.append("DRY RUN\n");
        }

        var grpBUtvalg = utvalgGruppeC(maxOTDatoLikEllerFoer, ressursSluttDatoEtter);
        var skalOppdateres = begrensUtvalgTilNavidentListe ?
                begrensUtvalgTilNavidenter(grpBUtvalg,  navidentListe) :
                ekskluderNavidenter(grpBUtvalg, navidentListe);

        sb.append("GruppeC - alle som har ressurs.Sluttdato etter ").append(ressursSluttDatoEtter)
            .append(", og ikke har aktiv OT etter ").append(maxOTDatoLikEllerFoer).append("\n");
        sb.append(begrensUtvalgTilNavidentListe ? "Antall navidenter å begrense utvalget med: " : "Antall navidenter å ekskludere: ").append(navidentListe.size()).append("\n");
        sb.append("Antall i Gruppe C utvalg: ").append(grpBUtvalg.size()).append("\n");
        sb.append("Antall å oppdatere: ").append(skalOppdateres.size()).append("\n");

        var inputMetadata = new InputMetadata(
            "nom-api MastreStatligeINom",
            "oppdaterRessursSluttdatoGrpC",
            Map.of("maxOTDat0LikEllerFoer", maxOTDatoLikEllerFoer, "ressursSluttDatoEtter", ressursSluttDatoEtter, "navidentListe", navidentListe, "begrensUtvalgTilNavidentListe", begrensUtvalgTilNavidentListe),
            new Origination(userNavident, "nom-api")
        );

        oppdaterRessursSluttdato(commit, skalOppdateres, inputMetadata);

        sb.append("\nnavident, sektor, startdato, sluttdato, maxOtSluttDato\n");
        skalOppdateres.forEach(getRessursRow(sb));

        log.info(sb.toString());
        return ResponseEntity.ok(sb.toString());
    }

    protected String markerStatligeSomMastret(boolean commit, List<String> ekskluderNavidenter) {
        StringBuilder sb = new StringBuilder();
        if (!commit) {
            sb.append("DRY RUN\n");
        }
        sb.append("Antall navidenter å ekskluderer: ").append(ekskluderNavidenter.size()).append("\n");

        var ikkeMastretStatlige = ressursStateRepository.getAll().stream()
            .filter(RessursState::isStatlig)
            .filter(ressurs -> !nomMastered.isMastered(ressurs.navident()))
            .toList();

        var ikkeMastretUtenEkskluderteNavidenter = ikkeMastretStatlige.stream()
            .filter(ressurs -> !ekskluderNavidenter.contains(ressurs.navident()))
            .toList();

        sb.append("Antall å mastre: ").append(ikkeMastretStatlige.size()).append("\n");
        sb.append("Antall å mastre uten ekskluderte navidenter: ").append(ikkeMastretUtenEkskluderteNavidenter.size()).append("\n");

        if (commit) {
            mastreNavidenter(ikkeMastretUtenEkskluderteNavidenter);
        }
        return sb.toString();
    }

    protected String oppdaterRessursSluttdato(boolean commit, List<Ressurs> ressurser, InputMetadata inputMetadata) {
        StringBuilder sb = new StringBuilder();
        if (!commit) {
            sb.append("DRY RUN\n");
        }

        sb.append("Antall navidenter å oppdatere sluttDato på samt trigge synk til IR: ").append(ressurser.size()).append("\n");

        if ( commit ) {
            ressurser.forEach(ressurs ->
                ressursCommandService.oppdaterRessursSluttdatoEvent(
                    new OppdaterSluttdatoCommand(
                        ressurs.fid(),
                        ressurs.maxOtSluttDato().equals(MAX_DATE) ? Optional.empty() : Optional.of(ressurs.maxOtSluttDato()),
                        inputMetadata
                    )
                )
            );
        }
        return sb.toString();
    }

    List<Ressurs> utvalgGruppeA(LocalDate maxOTDateEtter) {
        // Statlige, mastret og
        // seneste oT.tomDato > 'maxOTDate'  <- Ta med kun de er aktive etter denne datoen ('fremtidige')
        return getAlleStatligeMastrede().stream()
            .filter(ressurs -> ressurs.maxOtSluttDato() != null && !ressurs.maxOtSluttDato.equals(MAX_DATE) && etter(ressurs.maxOtSluttDato(), maxOTDateEtter))
            .toList();
    }

    List<Ressurs> utvalgGruppeB(LocalDate maxOTDateTLikEllerFoer, LocalDate maxOTDateLikEllerEtter) {
        // Statlige, mastret og
        // ressurs.sluttDato == NULL
        // og seneste oT.tomDato <= 'maxOTDateLikEllerEtter'    <- Ekskludere Gruppe A (Samme dato som brukt i gruppe A)
        // og seneste oT.tomDato >= 'maxOTDateLikEllerEtter'    <- Ekskluder gamle datoer - bruk 2024.12.01
        return getAlleStatligeMastrede().stream()
            .filter(ressurs -> ressurs.sluttDato() == null)
            .filter(ressurs -> ressurs.maxOtSluttDato() != null && !ressurs.maxOtSluttDato.equals(MAX_DATE) && likEllerFoer(ressurs.maxOtSluttDato(), maxOTDateTLikEllerFoer))
            .filter(ressurs -> likEllerEtter(ressurs.maxOtSluttDato(), maxOTDateLikEllerEtter))
            .toList();
    }

    List<Ressurs> utvalgGruppeC(LocalDate maxOTDateTLikEllerFoer, LocalDate ressursSluttDatoEtter) {
        // Statlige, mastret og
        // ressurs.sluttDato == NULL
        // og seneste oT.tomDato <= 'maxOTDateLikEllerEtter'    <- Ekskludere Gruppe A (Samme dato som brukt i gruppe A)
        // og ressurs.sluttDato > 'maxOTDateLikEllerEtter'    <- Ekskluder gamle datoer - bruk 2024.12.31
        return getAlleStatligeMastrede().stream()
            .filter(ressurs -> ressurs.maxOtSluttDato() != null && !ressurs.maxOtSluttDato.equals(MAX_DATE) && likEllerFoer(ressurs.maxOtSluttDato(), maxOTDateTLikEllerFoer))
            .filter(ressurs -> ressurs.sluttDato() != null && etter(ressurs.sluttDato(), ressursSluttDatoEtter))
            .toList();
    }


    List<Ressurs> getAlleStatligeSomIkkeErMastret() {
        return ressursStateRepository.getAll().stream()
            .filter(RessursState::isStatlig)
            .filter(ressurs -> !nomMastered.isMastered(ressurs.navident()))
            .map(ressurs -> {
                var maxOtSluttDato = getMaxOTDate(ressurs.navident());
                return new Ressurs(
                    ressurs.fid(),
                    ressurs.navident(),
                    ressurs.sektor(),
                    ressurs.startdato().orElse(null),
                    ressurs.sluttdato().orElse(null),
                    maxOtSluttDato
                );
            })
            .toList();
    }

    List<Ressurs> getAlleStatligeMastrede() {
        return ressursStateRepository.getAll().stream()
            .filter(RessursState::isStatlig)
            .filter(ressurs -> nomMastered.isMastered(ressurs.navident()))
            .map(ressurs -> {
                var maxOtSluttDato = getMaxOTDate(ressurs.navident());
                return new Ressurs(
                    ressurs.fid(),
                    ressurs.navident(),
                    ressurs.sektor(),
                    ressurs.startdato().orElse(null),
                    ressurs.sluttdato().orElse(null),
                    maxOtSluttDato
                );
            })
            .toList();
    }

    LocalDate getMaxOTDate(String navident) {
        return orgtilknytningService.getByNavIdent(navident).stream()
            .map(orgtilknytning -> orgtilknytning.gyldigTom().orElse(MAX_DATE))
            .max(LocalDate::compareTo).orElse(null);
    }


    private static List<Ressurs> begrensUtvalgTilNavidenter(List<Ressurs> ressurser, List<String> navidenter) {
        return ressurser.stream()
            .filter(ressurs -> navidenter.contains(ressurs.navident))
            .toList();
    }

    private static List<Ressurs> ekskluderNavidenter(List<Ressurs> ressurser, List<String> ekskluderteNavidenter) {
        return ressurser.stream()
            .filter(ressurs -> !ekskluderteNavidenter.contains(ressurs.navident))
            .toList();
    }

    void mastreNavidenter(List<RessursState> ressurserTilMastres) {
        ressurserTilMastres.forEach(ressurs -> nomMastered.markRessursAsMasteredInNom(ressurs.navident()));
    }

    record Ressurs(
        String fid,
        String navident,
        Sektor sektor,
        LocalDate startDato,
        LocalDate sluttDato,
        LocalDate maxOtSluttDato    // Null, ingen OT'er. OT uten sluttdato = LocalDate.MAX
    ) {}


    static Consumer<Ressurs> getRessursRow(StringBuffer sb) {
        return ressurs -> {
            sb.append(ressurs.navident()).append(", ");
            sb.append(ressurs.sektor()).append(", ");
            sb.append(ressurs.startDato()).append(", ");
            sb.append(ressurs.sluttDato()).append(", ");
            sb.append(ressurs.maxOtSluttDato()).append("\n");
        };
    }
}
