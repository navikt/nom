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
import no.nav.nom.contract.nom.Sektor;
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
import java.util.function.Consumer;

@Slf4j
@Tag(name = "Admin for å migrering til NOM som master av statlige ressurser")
@RestController
@RequestMapping("/api/admin/mastrestatlige")
@PreAuthorize("hasAuthority('ADMIN')")
public class MastreStatligeINom {

    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final NomMastered nomMastered;

    public MastreStatligeINom(RessursStateRepository ressursStateRepository, OrgtilknytningService orgtilknytningService, NomMastered nomMastered) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
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
        description = "List alle statlige ressurser som ikke er mastret i NOM, men som ekskluderes pga max OT før april 2025",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @GetMapping(value = "/listStatligeSomIkkeErMastretOgSomEkskluderesPaaMaksOTDato")
    public ResponseEntity<String> listStatligeSomIkkeErMastretOgSomEkskluderesPaaMaksOTDato() {
        StringBuffer sb = new StringBuffer();
        sb.append("navident, sektor, startdato, sluttdato, maxOtSluttDato\n");

        var ikkeMastret = getAlleStatligeSomIkkeErMastret();
        var statligeUtenDeEkskluderteNavidenter = eksludertDeMedMaxOTFoerApril2025(ikkeMastret).stream().map(Ressurs::navident).toList();

        ikkeMastret.stream()
            .filter(ressurs -> !statligeUtenDeEkskluderteNavidenter.contains(ressurs.navident()))
            .forEach(getRessursRow(sb));

        return ResponseEntity.ok(sb.toString());
    }

    static Consumer<Ressurs> getRessursRow(StringBuffer sb) {
        return ressurs -> {
            sb.append(ressurs.navident()).append(", ");
            sb.append(ressurs.sektor()).append(", ");
            sb.append(ressurs.startDato()).append(", ");
            sb.append(ressurs.sluttDato()).append(", ");
            sb.append(ressurs.maxOtSluttDato()).append("\n");
        };
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
        @Parameter(description = "Steg 1 - ekskluder de de med max OT-sluttdato 31.03.2025\n"
                                +"Steg 2 - kjør for alle") @RequestParam int steg,
        @RequestBody List<String> ekskluderNavidenter
    ) {
        return ResponseEntity.ok(markerStatligeSomMastret("CONFIRM".equalsIgnoreCase(confirm), steg, ekskluderNavidenter));
    }

    List<Ressurs> getAlleStatligeSomIkkeErMastret() {
        return ressursStateRepository.getAll().stream()
            .filter(RessursState::isStatlig)
            .filter(ressurs -> !nomMastered.isMastered(ressurs.navident()))
            .map(ressurs -> {
                var maxOtSluttDato = getMaxOTDate(ressurs.navident());
                return new Ressurs(
                    ressurs.navident(),
                    ressurs.sektor(),
                    ressurs.startdato().orElse(null),
                    ressurs.sluttdato().orElse(null),
                    maxOtSluttDato
                );
            })
            .toList();
    }

    static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    LocalDate getMaxOTDate(String navident) {
        return orgtilknytningService.getByNavIdent(navident).stream()
            .map(orgtilknytning -> orgtilknytning.gyldigTom().orElse(MAX_DATE))
            .max(LocalDate::compareTo).orElse(null);
    }

    protected String markerStatligeSomMastret(boolean commit, int steg, List<String> ekskluderNavidenter) {
        StringBuilder sb = new StringBuilder();
        if (!commit) {
            sb.append("DRY RUN");
        }
        sb.append("Antall navidenter å ekskluderer: ").append(ekskluderNavidenter.size()).append(" navidenter\n");

        var ikkeMastretStatlige = getAlleStatligeSomIkkeErMastret();

        var ikkeMastretUtenEkskluderteNavidenter = ikkeMastretStatlige.stream()
            .filter(ressurs -> !ekskluderNavidenter.contains(ressurs.navident()))
            .toList();

        sb.append("Antall ikke mastret: ").append(ikkeMastretStatlige.size()).append("\n");
        sb.append("Antall ikke mastret uten ekskluderte navidenter: ").append(ikkeMastretUtenEkskluderteNavidenter.size()).append("\n");

        var ekskludertOTFoerApril2025 = eksludertDeMedMaxOTFoerApril2025(ikkeMastretUtenEkskluderteNavidenter);

        if (steg == 1) {
            sb.append("Steg 1 antall ekskluderert på OT dato:").append(ikkeMastretUtenEkskluderteNavidenter.size() - ekskludertOTFoerApril2025.size()).append("\n");
            sb.append("Antall å mastre i steg 1: ").append(ekskludertOTFoerApril2025.size()).append("\n");
            if (commit) {
                mastreNavidenter(ekskludertOTFoerApril2025);
            }
        } else {
            sb.append("Antall å mastre i steg 2: ").append(ikkeMastretUtenEkskluderteNavidenter.size()).append("\n");
            if (commit) {
                mastreNavidenter(ikkeMastretUtenEkskluderteNavidenter);
            }
        }
        return sb.toString();
    }

    private static List<Ressurs> eksludertDeMedMaxOTFoerApril2025(List<Ressurs> ikkeMastretUtenEkskluderteNavidenter) {
        return ikkeMastretUtenEkskluderteNavidenter.stream()
            .filter(ressurs -> ressurs.maxOtSluttDato() == null || ressurs.maxOtSluttDato().isAfter(LocalDate.of(2025, 3, 31)))
            .toList();
    }

    void mastreNavidenter(List<Ressurs> ressurserTilMastres) {
        ressurserTilMastres.forEach(ressurs -> nomMastered.markRessursAsMasteredInNom(ressurs.navident()));
    }

    record Ressurs(
        String navident,
        Sektor sektor,
        LocalDate startDato,
        LocalDate sluttDato,
        LocalDate maxOtSluttDato    // Null, ingen OT'er. OT uten sluttdato = LocalDate.MAX
    ) {}
}
