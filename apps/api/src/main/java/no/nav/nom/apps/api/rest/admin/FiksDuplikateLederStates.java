package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederEventProducer;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.SlettOrgEnhetLeder;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/fiksDuplikateLederStates")
@PreAuthorize("hasAuthority('ADMIN')")
public class FiksDuplikateLederStates {
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;

    public FiksDuplikateLederStates(
        OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        OrgEnhetLederCommandService orgEnhetLederCommandService
    ) {
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.orgEnhetLederCommandService = orgEnhetLederCommandService;
    }

    @Operation(
        description = "Admin for å finne og fjerne duplikate leder-states",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    public String fiksDuplikateLederStates(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        StringBuffer output = new StringBuffer();
        boolean performUpdate = "CONFIRM".equals(confirm);

        // Finn duplikate leder states
        var alleLederStatesPerNavidentEnhetOgPeriode =
            orgEnhetLederStateRepository.getAll()
                .stream()
                .collect(Collectors.groupingBy(FiksDuplikateLederStates::getLederStateGroupingKey));

        List<String> fidsToDelete = new ArrayList<>();

        alleLederStatesPerNavidentEnhetOgPeriode.forEach((key, lederStates) -> {
            if (lederStates.size() > 1) {
                var fids = lederStates.stream().skip(1).map(OrgEnhetLederState::fid).toList();
                output.append("Duplikate leder states (%s) for %s:\n".formatted(lederStates.size(), key));
                output.append("Vil slette %s leder states:\n".formatted(fids.size(), key));
                fidsToDelete.addAll(fids);
            }
        });

        var inputMetaData = new InputMetadata("/api/admin/fiksDuplikateLederStates", "fiksDuplikateLederStates", null, new Origination("nom-api", "nom-api"));

        if (performUpdate) {
            fidsToDelete.forEach(fid ->
                orgEnhetLederCommandService.slettOrgEnhetLederState(
                    new SlettOrgEnhetLeder(
                        fid, inputMetaData
                    )
                )
            );
            output.append("Utført sletting av %s duplikate leder states".formatted(fidsToDelete.size()));
        } else {
            output.append("DRYRUN Lederstates å slette: %s".formatted(fidsToDelete.size()));
        }

        // Sjekk etter overlappende leder states
        var alleLederStatesPerNavidentEnhet =
            orgEnhetLederStateRepository.getAll()
                .stream()
                .sorted(Comparator
                    .comparing(OrgEnhetLederState::startdato, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(OrgEnhetLederState::sluttdato, Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.groupingBy(lederState -> lederState.navident() + "_" + lederState.orgEnhetId()));

        List<String> coveredFidsToDelete= new ArrayList<>();
        alleLederStatesPerNavidentEnhet.forEach((key, lederStates) -> {
            List<String> fidsCovered = new ArrayList<>();
            lederStates.forEach(state -> {
                if (isCovered(state, lederStates)) {
                    fidsCovered.add(state.fid());
                }
                ;
            });
            if (!fidsCovered.isEmpty()) {
                output.append("Overlappende leder states å slette for %s (%s): %s\n".formatted(key, fidsCovered.size(), fidsCovered));
                coveredFidsToDelete.addAll(fidsCovered);
            }
        });

        if (performUpdate) {
            coveredFidsToDelete.forEach(fid ->
                orgEnhetLederCommandService.slettOrgEnhetLederState(
                    new SlettOrgEnhetLeder(
                        fid, inputMetaData
                    )
                )
            );
            output.append("Utført sletting av %s duplikate overlappende  leder states".formatted(fidsToDelete.size()));
        } else {
            output.append("DRYRUN overlappende Leder states å slette: %s".formatted(fidsToDelete.size()));
        }

        return output.toString();
    }

    private boolean isCovered(OrgEnhetLederState state, List<OrgEnhetLederState> lederStates) {
        return lederStates.stream()
            .anyMatch(otherState ->
                    !Objects.equals(otherState.fid(), state.fid())
                    && PeriodeUtil.intervalCoversDate(otherState.startdato(), otherState.sluttdato(), state.startdato())
                    && PeriodeUtil.intervalCoversDate(otherState.startdato(), otherState.sluttdato(), state.sluttdato())
                    );
    }

    private static @NonNull String getLederStateGroupingKey(OrgEnhetLederState lederState) {
        return lederState.navident() + "_" + lederState.orgEnhetId() + "_" + lederState.startdato() + "_" + lederState.sluttdato();
    }
}
