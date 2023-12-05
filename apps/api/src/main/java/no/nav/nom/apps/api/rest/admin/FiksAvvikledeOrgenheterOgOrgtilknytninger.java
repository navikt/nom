package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/korrigereAvvikledeEnheterOgOrgtilknytninger")
public class FiksAvvikledeOrgenheterOgOrgtilknytninger {
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final RessursStateRepository ressursStateRepository;

    public FiksAvvikledeOrgenheterOgOrgtilknytninger(OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService,
                                                     RessursStateRepository ressursStateRepository
    ) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.ressursStateRepository = ressursStateRepository;
    }

    @Operation(
        description = "Admin for å fikse avsluttede enheter (endret navn) og orgtilknytninger",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    public String dataSanityCheckEnheter(@RequestParam(defaultValue = "false") boolean performUpdate, @RequestParam(defaultValue = "false") boolean inkluderEksterne) {
        // Finn alle orgtilknytninger til orgenhet som er avsluttet eller ikke er i hierarkiet
        Collection<IOrgenhet> aktiveEnheter = orgenhetService.getAllEnheterMedOrganisering();
        List<String> aktiveEnheterIds = aktiveEnheter.stream().map(IOrgenhet::getId).toList();
        List<IOrgenhet> ikkeAktiveEnheter = orgenhetService.getAllEnheter().stream().filter(o -> !aktiveEnheterIds.contains(o.getId())).toList();
        List<String> ikkeAktiveEnheterIds = ikkeAktiveEnheter.stream().map(IOrgenhet::getId).toList();

        List<RessursState> aktuelleAnsatte = ressursStateRepository.getAll().stream()
            .filter(r -> inkluderEksterne || !r.sektor().equals(Sektor.EKSTERN))
            .toList();

        List<String> aktuelleNavidenter = aktuelleAnsatte.stream().map(RessursState::navident).toList();

        List<OrgtilknytningState> aktiveOrgtilknytningerTilIkkeAktiveEnheter = orgtilknytningService.getAll().stream()
            .filter(ot -> ikkeAktiveEnheterIds.contains(ot.orgEnhetId()) && aktuelleNavidenter.contains(ot.navident()))
            .toList();

        // Finn alle orgtilknytninger til alle Ansatte som har har OT til ikke-aktiv Enhet
        StringBuffer output = new StringBuffer();
        aktuelleNavidenter.forEach(r -> {
            output.append("%s\n".formatted(r));
            aktiveOrgtilknytningerTilIkkeAktiveEnheter.stream()
                .filter(ot -> ot.navident().equals(r))
                .forEach(ot -> {
                    output.append("  %s -> %s - %s\n".formatted(getEnhetText(ot.orgEnhetId()), ot.gyldigFom(), ot.gyldigTom()));
                });
        });

        return output.toString();
    }

    private String getEnhetText(String orgenhetId) {
        StringBuffer enhetStr = new StringBuffer();
        var enhet = orgenhetService.getById(orgenhetId).get();
        enhetStr.append(enhet.getId())
            .append(" - ")
            .append(enhet.getNavn())
            .append("[")
            .append(enhet.getUniqueAgressoId())
            .append("], (")
            .append(enhet.getGyldigFom())
            .append(" - ")
            .append(enhet.getGyldigTom().isPresent() ? enhet.getGyldigTom().get() : " ");
        return enhetStr.toString();
    }
}
