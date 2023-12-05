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

import java.time.LocalDate;
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
    public String korrigereAvvikledeEnheterOgOrgtilknytninger(@RequestParam(defaultValue = "false") boolean performUpdate, @RequestParam(defaultValue = "false") boolean inkluderEksterne) {
        StringBuffer output = new StringBuffer();

        // Finn alle orgtilknytninger til orgenhet som er avsluttet eller ikke er i hierarkiet
        Collection<IOrgenhet> aktiveEnheter = orgenhetService.getAllEnheterMedOrganisering();
        List<String> aktiveEnheterIds = aktiveEnheter.stream().map(IOrgenhet::getId).toList();
        List<IOrgenhet> ikkeAktiveEnheter = orgenhetService.getAllEnheter().stream().filter(o -> !aktiveEnheterIds.contains(o.getId())).toList();
        List<String> ikkeAktiveEnheterIds = ikkeAktiveEnheter.stream().map(IOrgenhet::getId).toList();

        List<RessursState> aktuellaktive= ressursStateRepository.getAll().stream()
            .filter(r -> inkluderEksterne || !r.sektor().equals(Sektor.EKSTERN))
            .filter(r -> r.sluttdato().isEmpty() || r.sluttdato().get().isAfter(LocalDate.now().minusYears(1)))
            .toList();

        List<String> aktiveNavidenter = aktuellaktive.stream().map(RessursState::navident).toList();

        List<OrgtilknytningState> aktiveOrgtilknytningerTilIkkeAktiveEnheter = orgtilknytningService.getAll().stream()
            .filter(ot -> ikkeAktiveEnheterIds.contains(ot.orgEnhetId()) && aktiveNavidenter.contains(ot.navident()))
            .toList();
        List<String> aktiveOrgtilknytningerTilIkkeAktiveEnheterFids = aktiveOrgtilknytningerTilIkkeAktiveEnheter.stream().map(OrgtilknytningState::fid).toList();

        List<String> aktuelleNavidenter = aktiveOrgtilknytningerTilIkkeAktiveEnheter.stream().map(OrgtilknytningState::navident).toList();

        output.append("Antall aktive enheter: %s\n".formatted(aktiveEnheter.size()));
        output.append("Antall ikke aktive enheter: %s\n".formatted(ikkeAktiveEnheter.size()));
        output.append("Antall aktive orgtilknytninger til ikke aktive enheter: %s\n".formatted(aktiveOrgtilknytningerTilIkkeAktiveEnheter.size()));
        output.append("Antall aktive ansatte: %s\n".formatted(aktuellaktive.size()));
        output.append("Antall aktuelle ansatte: %s\n".formatted(aktuellaktive.size()));
        output.append("\n");

        // Finn alle orgtilknytninger til alle Ansatte som har har OT til ikke-aktiv Enhet
        aktuelleNavidenter.forEach(navident -> {
            output.append("%s\n".formatted(navident));
            orgtilknytningService.getByNavIdent(navident).stream()
                .filter(ot -> ot.navident().equals(navident))
                .forEach(ot -> {
                    output.append(aktiveOrgtilknytningerTilIkkeAktiveEnheterFids.contains(ot.fid()) ? "  *" : "   ");
                    output.append("    %s  =>  %s - %s\n".formatted(getEnhetText(ot.orgEnhetId()), ot.gyldigFom(), ot.gyldigTom().isEmpty() ? " " : ot.gyldigTom().get()));
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
            .append(enhet.getGyldigTom().isPresent() ? enhet.getGyldigTom().get() : ")");
        return enhetStr.toString();
    }
}
