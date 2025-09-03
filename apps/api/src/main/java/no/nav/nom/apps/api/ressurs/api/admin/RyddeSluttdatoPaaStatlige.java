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
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.*;

@Slf4j
@Tag(name = "Admin for å scripte fjerning av sluttdato på statlige ressurser")
@RestController
@RequestMapping("/api/admin/ryddesluttdato")
@PreAuthorize("hasAuthority('ADMIN')")
public class RyddeSluttdatoPaaStatlige {
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final RessursCommandService ressursCommandService;
    private final NomMastered nomMastered;

    static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    public RyddeSluttdatoPaaStatlige(RessursStateRepository ressursStateRepository, OrgtilknytningService orgtilknytningService,
                                     RessursCommandService ressursCommandService, NomMastered nomMastered) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.ressursCommandService = ressursCommandService;
        this.nomMastered = nomMastered;
    }

    @Operation(
        description = "Fjerner sluttDato på ressurs og siste OT for Statlig, fast ansatte",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PostMapping(value = "/fjerneSluttdato")
    public ResponseEntity<String> fjerneSluttdato(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @Parameter(description = "Behandle kun de med sluttdato etter") @RequestParam LocalDate maxOTDatoEtter,
        @RequestBody List<String> ekskluderNavidenter
    ) {
        boolean commit = "CONFIRM".equalsIgnoreCase(confirm);

        // Hent ressurser med sluttdato etter gitt dato
        // Som er statlige og har siste OT som er FAST
        // Og som ikke er ekskludert av angitt liste

        var ressurserIUtvalg = getAlleStatligeMastrede().stream()
            .filter(ressurs -> ressurs.sluttDato() != null && etter(ressurs.sluttDato(), maxOTDatoEtter))
            .filter(ressurs -> !ekskluderNavidenter.contains(ressurs.navident))
            .filter(this::harTilknytningstypeFastPaaSisteOT)
            .sorted(Comparator.comparing(Ressurs::navident))
            .toList();

        StringBuffer result = new StringBuffer();
        if (!commit) {
            result.append("DRY RUN!\n\n");
        }

        result.append("Utvalg med flere enn en 'siste' OT\n").append(listRessurserMedFlerEnnEnSisteOt(ressurserIUtvalg)).append("\n\n");
        result.append("Utvalg med max en 'siste' OT\n").append(listRessurserMedMaxEnSisteOt(ressurserIUtvalg)).append("\n\n");

        return ResponseEntity.ok(result.toString());
    }

    List<Ressurs> getAlleStatligeMastrede() {
        return ressursStateRepository.getAll().stream()
            .filter(RessursState::isStatlig)
            .filter(ressurs -> nomMastered.isMastered(ressurs.navident()))
            .map(ressurs -> {
                var sisteOTer = getSisteOTer(ressurs.navident());
                return new Ressurs(
                    ressurs.fid(),
                    ressurs.navident(),
                    ressurs.sektor(),
                    ressurs.startdato(),
                    ressurs.sluttdato(),
                    sisteOTer
                );
            })
            .toList();
    }

    List<OrgtilknytningState> getSisteOTer(String navident) {
        final var alleOter = orgtilknytningService.getByNavIdent(navident);
        final var maxOtDate =
            alleOter.stream()
                .map(orgtilknytning -> requireNonNullElse(orgtilknytning.gyldigTom(), MAX_DATE))
                .max(LocalDate::compareTo).orElse(null);

        if (maxOtDate == null) {
            return List.of();
        }

        return alleOter.stream()
            .filter(ot -> Objects.equals(maxOtDate.equals(MAX_DATE) ? null : maxOtDate, ot.gyldigTom()))
            .toList();
    }

    private boolean harTilknytningstypeFastPaaSisteOT(Ressurs ressurs) {
        return ressurs.sisteOTer.stream().anyMatch(ot -> Tilknytningtype.FAST.equals(ot.orgtilknytningType()));
    }

    private String listRessurserMedFlerEnnEnSisteOt(List<Ressurs> ressurserIUtvalg) {
        StringBuffer sb = new StringBuffer();
        ressurserIUtvalg.stream()
            .filter(ressurs -> ressurs.sisteOTer.size() > 1)
            .forEach(ressurs -> {
                sb.append(ressurs.navident).append(", ");
                sb.append(ressurs.sektor).append(", ");
                sb.append(ressurs.startDato).append(", ");
                sb.append(ressurs.sluttDato).append("\n");
                ressurs.sisteOTer.forEach(ot -> {
                    sb.append("\t");
                    sb.append(ot.orgtilknytningType()).append(", ");
                    sb.append(ot.sektor()).append(", ");
                    sb.append(ot.orgEnhetId()).append(", ");
                    sb.append(ot.gyldigFom()).append(", ");
                    sb.append(ot.gyldigTom()).append(", ");
                });
            });
        return sb.toString();
    }

    private String listRessurserMedMaxEnSisteOt(List<Ressurs> ressurserIUtvalg) {
        StringBuffer sb = new StringBuffer();
        ressurserIUtvalg.stream()
            .filter(ressurs -> ressurs.sisteOTer.size() > 1)
            .forEach(ressurs -> {
                sb.append(ressurs.navident).append(", ");
                sb.append(ressurs.sektor).append(", ");
                sb.append(ressurs.startDato).append(", ");
                sb.append(ressurs.sluttDato).append(", ");
                ressurs.sisteOTer.forEach(ot -> {
                    sb.append(ot.orgtilknytningType()).append(", ");
                    sb.append(ot.sektor()).append(", ");
                    sb.append(ot.orgEnhetId()).append(", ");
                    sb.append(ot.gyldigFom()).append(", ");
                    sb.append(ot.gyldigTom()).append(", ");
                });
            });
        return sb.toString();
    }

    record Ressurs(
        String fid,
        String navident,
        Sektor sektor,
        LocalDate startDato,
        LocalDate sluttDato,
        List<OrgtilknytningState> sisteOTer
    ) {}
}
