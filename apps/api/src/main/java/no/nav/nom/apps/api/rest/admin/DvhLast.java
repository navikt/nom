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
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å manuelt trigge dvh Last")
@RestController
@RequestMapping("/api/admin/dvhLast")
@PreAuthorize("hasAuthority('ADMIN')")
public class DvhLast {
    private final Logger log = LoggerFactory.getLogger(DvhLast.class);

    private final DvhRestConsumer dvhRestConsumer;
    private final DvhOrgenhetsLast dvhOrgenhetsLast;
    private final OrgEnhetTypeHandler orgEnhetTypeHandler;

    private final RessursStillingLoadStatus ressursStillingLoadStatus;


    public DvhLast(
        DvhRestConsumer dvhRestConsumer,
        DvhOrgenhetsLast dvhOrgenhetsLast,
        OrgEnhetTypeHandler orgEnhetTypeHandler,
        RessursStillingLoadStatus ressursStillingLoadStatus
    ) {
        this.dvhRestConsumer = dvhRestConsumer;
        this.dvhOrgenhetsLast = dvhOrgenhetsLast;
        this.orgEnhetTypeHandler = orgEnhetTypeHandler;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
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
        triggerKoblingerOgLederLast();
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
        description = "Last av skygge orgtilknytninger for kommunale (OMADA)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/loadSkyggeOrgtilknytningerForKommunale")
    public void loadSkyggeOrgtilknytningerForKommunale() {
        triggerlastSkyggeOrgtilknytningerForKommunale();
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
        triggerKoblingerOgLederLast();
        triggerOrgEnhetsTypeLast();
    }

    @Operation(
        description = "Få status på siste Last av orgtilknytninger",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/orgtilknytningerLastStatus")
    public String orgtilknytningerLastStatus(@RequestParam(required = false) String navident) {
        return ressursStillingLoadStatus.report(navident);
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
    public void triggerKoblingerOgLederLast() {
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();
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
    public void triggerlastSkyggeOrgtilknytningerForKommunale() {
        dvhRestConsumer.updateShadowOrgtilknytningerForKommunaleFromDvh();
    }

    @Async
    public void triggerOrgEnhetsTypeLast(){
        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();
    }
}
