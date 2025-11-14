package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å manuelt trigge dvh Last")
@RestController
@RequestMapping("/api/admin/dvhLast")
@PreAuthorize("hasAuthority('ADMIN')")
public class DvhLast {

    private final DvhRestConsumer dvhRestConsumer;
    private final DvhOrgenhetsLast dvhOrgenhetsLast;
    private final OrgEnhetTypeHandler orgEnhetTypeHandler;

    public DvhLast(
        DvhRestConsumer dvhRestConsumer,
        DvhOrgenhetsLast dvhOrgenhetsLast,
        OrgEnhetTypeHandler orgEnhetTypeHandler
    ) {
        this.dvhRestConsumer = dvhRestConsumer;
        this.dvhOrgenhetsLast = dvhOrgenhetsLast;
        this.orgEnhetTypeHandler = orgEnhetTypeHandler;
    }

    @Operation(
        description = "Start last av orgEnheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/loadOrgEnheter")
    public void loadOrgEnheter() {
        triggeOrgEnhetsLast();
    }

    @Operation(
        description = "Start last av ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/loadRessurser")
    public void loadRessurser() {
        triggerRessursLast();
    }

    @Operation(
        description = "Start last av organisering, koblinger og ledere",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/loadOrganiseringKoblingerOgLedere")
    public void loadOrganiseringKoblingerOgLedere() {
        triggerOrganiseringerOgLederLast();
    }

    @Operation(
        description = "Start last av kun orgtilknytninger",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/loadOrgtilknytninger")
    public void loadOrgtilknytninger() {
        triggerlastOrgtilknytningerTilEvtsrc();
    }

    @Operation(
        description = "Last av orgtilknytninger for eksterne",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/loadOrgtilknytningerForEksterne")
    public void loadOrgtilknytningerForEksterne() {
        triggerlastEksterneOrgtilknytningerTilEvtsrc();
    }

    @Operation(
        description = "Start last av typer for orgenheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/resolveOrgEnhetsTyper")
    public void resolveOrgEnhetsTyper() {
        triggerOrganiseringerOgLederLast();
        triggerOrgEnhetsTypeLast();
    }

    @Operation(
        description = "Trigger sjekk av OT'er utenfor hierarki",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/findOTUtenforHierarki")
    public void findOTUtenforHierarki() {
        triggerFindOTUtenforHierarki();
    }

    @Async
    public void triggerRessursLast() {
        dvhRestConsumer.loadRessurserFromDvh();
    }

    @Async
    public void triggeOrgEnhetsLast() {
        dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();
    }

    @Async
    public void triggerOrganiseringerOgLederLast() {
        dvhRestConsumer.loadLedereFromDvh();
        dvhRestConsumer.loadOrganiseringerFromDvh(true);
    }

    @Async
    public void triggerlastOrgtilknytningerTilEvtsrc() {
        dvhRestConsumer.lastOrgtilknytningerTilEvtsrc();
    }

    @Async
    public void triggerlastEksterneOrgtilknytningerTilEvtsrc() {
        dvhRestConsumer.updateOrgtilknytningerForEksterneFromDvh();
    }

    @Async
    public void triggerOrgEnhetsTypeLast(){
        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();
    }

    @Async
    public void triggerFindOTUtenforHierarki() {
        dvhRestConsumer.findOTUtenforHierarki();
    }
}
