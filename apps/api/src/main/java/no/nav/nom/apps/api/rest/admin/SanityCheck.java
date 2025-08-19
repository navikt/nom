package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Tag(name = "Admin for å kjøre en sanity check på data")
@RestController
@RequestMapping("/api/admin/SanityCheck")
@PreAuthorize("hasAuthority('ADMIN')")
public class SanityCheck {
    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;

    public SanityCheck(
        DvhRepository dvhRepository,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService
    ) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
    }

    @Operation(
        description = "List orgenher i NAV orghierarkiet som ikke har leder",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listOrgenheterUtenLeder")
    public String listOrgenheterUtenLeder() {
        StringBuffer result = new StringBuffer();
        var alleEnheter = orgenhetService.getAllEnheterMedOrganisering().stream().toList(); // Aktive enheter i organisasjonstreet
        List<IOrgenhet> alleOrgEnheterUtenLeder = new ArrayList<>();
        for (IOrgenhet orgenhet : alleEnheter) {
            List<DvhLeder> leaders = dvhRepository.getLederByAgressoIdAndNivAndNavn(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn())
                .stream()
                .filter(Objects::nonNull)
                .toList();
            if (leaders.isEmpty()) {
                alleOrgEnheterUtenLeder.add(orgenhet);
            }
        }

        alleOrgEnheterUtenLeder.forEach(oe ->
            result.append(oe.getId())
                .append(";")
                .append(oe.getUniqueAgressoId())
                .append(";")
                .append(oe.getGyldigFom())
                .append(";")
                .append(oe.getGyldigTom().isPresent() ? oe.getGyldigTom().get() : "")
                .append("\n")
        );
        return result.toString();
    }


    @Operation(
        description = "Valider ressurs som har sluttet, men står som leder på orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/sluttetRessursMedLederRollePaaOrgEnhet")
    public List<EnhetMedRessursSomHarSluttetOgErLederSanityCheckResponse> validerRessursMedLederRollePaaOrgEnhet() {
        var alleEnheter = orgenhetService.getAllEnheterMedOrganisering().stream().toList(); // Aktive enheter i organisasjonstreet
        var alleRessurserWithSluttDatoBeforeToday = ressursStateRepository.findAllWithSluttdatoBeforeToday();
        Map<RessursState, List<IOrgenhet>> alleOrgEnheterMedLederSomHarSluttet = new HashMap<>();
        for (IOrgenhet orgenhet : alleEnheter) {
            validerEnhetsledereSomHarSluttet(orgenhet, alleRessurserWithSluttDatoBeforeToday).ifPresent(ressursState -> {
                if (!alleOrgEnheterMedLederSomHarSluttet.containsKey(ressursState)) {
                    alleOrgEnheterMedLederSomHarSluttet.computeIfAbsent(ressursState, org -> new ArrayList<>()).add(orgenhet);
                } else {
                    alleOrgEnheterMedLederSomHarSluttet.get(ressursState).add(orgenhet);
                }
            });
        }
        return EnhetMedRessursSomHarSluttetOgErLederSanityCheckResponse.from(alleOrgEnheterMedLederSomHarSluttet);
    }

    private <K, V> Map<K, List<V>> addOrPut(Map<K, List<V>> map, K key, V value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, new ArrayList<>());
            map.get(key).add(value);
        }
        return map;
    }

    private Optional<RessursState> validerEnhetsledereSomHarSluttet(IOrgenhet orgenhet, Map<String, RessursState> alleRessurserWithSluttDatoBeforeToday) {
        return dvhRepository.getLederByAgressoIdAndNivAndNavn(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn()).stream()
            .map(leder -> alleRessurserWithSluttDatoBeforeToday.get(leder.getNavIdent()))
            .filter(Objects::nonNull)
            .findFirst();
    }
}


