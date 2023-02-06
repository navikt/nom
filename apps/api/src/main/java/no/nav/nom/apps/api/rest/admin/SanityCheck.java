package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

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

        var orgEnheter = orgenhetService.getAllEnheter();
        var oeStream = orgEnheter.stream();

        result.append("Totalt antall orgEnheter: %s".formatted(oeStream.count()));
        result.append("   Med startdato: %s".formatted(oeStream.filter( oe -> oe.getGyldigFom() != null).count()));
        result.append("   Med sluttdato: %s".formatted(oeStream.filter( oe -> oe.getGyldigTom() != null).count()));
        return result.toString();
    }
}


