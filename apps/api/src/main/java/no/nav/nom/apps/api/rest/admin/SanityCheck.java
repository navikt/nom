package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Admin for å kjøre en sanity check på data")
@RestController
@RequestMapping("/api/v1/admin/SanityCheck")
public class SanityCheck {
    private final Logger log = LoggerFactory.getLogger(SanityCheck.class);

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    public SanityCheck(DvhRepository dvhRepository, IRessursService ressursService, IOrgenhetService orgenhetService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
    }

    @Operation(
        description = "Kjør Sanity Check på data",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/data")
    public String dataSanityCheck() {
        StringBuffer result = new StringBuffer();

        result.append(sjekkOrgEnhetStartSluttDato());

        return result.toString();
    }

    private String sjekkOrgEnhetStartSluttDato() {
        StringBuffer result = new StringBuffer();

        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<DvhOrganisering> alleOrganiseringer = dvhRepository.getOrganiseringer();

        result.append("Totalt antall alleEnheter: %s\n".formatted(alleEnheter.size()));
        result.append("   Med startdato: %s\n".formatted(enheterMedStartdato(alleEnheter).size()));
        result.append("   Med sluttdato: %s\n".formatted(enheterMedSluttdato(alleEnheter).size()));
        result.append("   Med sluttdato før startdato: %s\n\n".formatted(enheterMedSluttdatoFørStartdato(enheterMedSluttdato(alleEnheter)).size()));

        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
        List<DvhOrganisering> aktiveOrganiseringer = aktiveOrganiseringer(alleOrganiseringer);
        List<String> aktiveOverEnheterUniqueAgressoId = uniqueAgressoIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueAgressoId = uniqueAgressoIdForUnderEnhet(aktiveOrganiseringer);
        List<IOrgenhet> aktiveEnheterUtenAktivOverEnhet = enheterUtenEnhetOver(aktiveEnheter, aktiveOverEnheterUniqueAgressoId);
        List<IOrgenhet> aktiveEnheterUtenAktivUnderEnhet = enheterUtenEnhetOver(aktiveEnheter, aktiveUnderEnheterUniqueAgressoId);

        result.append("Aktive enheter: %s\n".formatted(aktiveEnheter.size()));
        result.append("Aktive enheter uten OverEnheter: %s\n".formatted(aktiveEnheterUtenAktivOverEnhet.size()));
        result.append("Aktive enheter uten UnderEnheter: %s\n".formatted(aktiveEnheterUtenAktivUnderEnhet.size()));

        return result.toString();
    }

    private List<IOrgenhet> enheterMedStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigFom() != null).toList();
    }

    private List<IOrgenhet> enheterMedSluttdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigTom().isPresent()).toList();
    }

    private List<IOrgenhet> enheterMedSluttdatoFørStartdato(List<IOrgenhet> enheter) {
        return enheter.stream().filter( oe -> oe.getGyldigTom().get().isBefore(oe.getGyldigFom())).toList();
    }

    private List<IOrgenhet> aktiveEnheter(List<IOrgenhet> enheter) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return enheter.stream().filter( oe ->
                oe.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (!oe.getGyldigTom().isPresent() || oe.getGyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<DvhOrganisering> getFremtidigeOrganiseringer(List<DvhOrganisering> organiseringer) {
        // FOM er fremtidig, samt at TOM er null eller etter FOM
        return organiseringer.stream().filter( o ->
                o.getGyldigFom().isAfter(LocalDate.now()) &&
                (o.getGyldigTom() != null || o.getGyldigTom().isAfter(o.getGyldigFom()))
        ).toList();
    }

    private List<DvhOrganisering> aktiveOrganiseringer(List<DvhOrganisering> organiseringer) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return organiseringer.stream().filter( o ->
                o.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (o.getGyldigTom() != null || o.getGyldigTom().isAfter(LocalDate.now()))
        ).toList();
    }

    public List<String> uniqueAgressoIdForOverEnhet(List<DvhOrganisering> organiseringer) {
        return organiseringer.stream().map(o -> o.uniqueAgressoIdOver()).toList();
    }

    public List<String> uniqueAgressoIdForUnderEnhet(List<DvhOrganisering> organiseringer) {
        return organiseringer.stream().map(o -> o.uniqueAgressoIdUnder()).toList();
    }

    private List<IOrgenhet> enheterUtenEnhetOver(List<IOrgenhet> enheter, List<String> underEnheterUniqueAgessoId) {
        // Enheter som ikke er under enhet i organiseringer
        return enheter.stream().filter( oe -> !underEnheterUniqueAgessoId.contains(oe.getUniqueAgressoId())).toList();
    }

}


