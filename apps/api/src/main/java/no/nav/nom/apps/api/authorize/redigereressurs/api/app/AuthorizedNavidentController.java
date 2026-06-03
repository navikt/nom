package no.nav.nom.apps.api.authorize.redigereressurs.api.app;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.authorize.redigereressurs.DelegationAdService;
import no.nav.nom.apps.api.authorize.redigereressurs.NotAuthorizedToDelegateLeder;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.state.LederTilgangState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Slf4j
@Tag(name = "Controller metode for å verifisere redigere ressurs tilgang og administrere delegerering av tilganger")
@RestController
@RequestMapping("/app/authorizedRessursRedigering")
public class AuthorizedNavidentController {
    private final AuthorizedNavident authorizedNavident;
    private final AuthResolver authResolver;
    private final DelegationAdService delegationAdService;

    public AuthorizedNavidentController(
        AuthorizedNavident authorizedNavident,
        AuthResolver authResolver,
        DelegationAdService delegationAdService
    ) {
        this.authorizedNavident = authorizedNavident;
        this.authResolver = authResolver;
        this.delegationAdService = delegationAdService;
    }

    @Operation(
        description = "om current bruker har tilgang til å redigere ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("isAuthorizedForNavident/{navident}")
    @PreAuthorize("hasAuthority('NOM_UI_APP')")
    boolean isAuthorizedForNavident(@PathVariable String navident) {
        return authResolver.kanRedigereRessurs(navident);

    }

    @Operation(
        description = "Om oppgitt bruker har tilgang til å redigere oppgitt ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("/isAuthorized")
    public boolean isAuthorizedForNavident(@RequestBody BrukerSkrivetilgangRessursRequest request) {
        return authorizedNavident.kanBrukerRedigereRessurs(request.bruker(), request.ressurs());
    }

    @Operation(
        description = "Om oppgitt bruker har tilgang til å redigere oppgitt ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("/isAuthorizedForNavidents")
    public boolean isAuthorizedForNavidents(@RequestBody BrukerSkrivetilgangRessurserRequest request) {
        log.info("isAuthorizedForNavidents: {}", request);
        for (String ressurs : request.ressurser()) {
            if (!authorizedNavident.kanBrukerRedigereRessurs(request.bruker(), ressurs)) {
                log.info("isAuthorizedForNavidents false: {}, {}", request.bruker(), ressurs);
                return false;
            }
        }
        log.info("isAuthorizedForNavidents true: {}, {}", request.bruker(), request.ressurser());
        return true;
    }

    @Operation(
        description = "om angitt bruker har tilgang til å opprette ressurser i angitt orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/hasNavidentAccessToResourcesForOrgenhet/{navIdent}/{orgenhetId}")
    boolean hasNavidentAccessToResourcesForOrgenhet(@PathVariable String navIdent, @PathVariable String orgenhetId) {
        log.info("hasNavidentAccessToResourcesForOrgenhet, navIdent: {}, orgenhetId: {}", navIdent, orgenhetId);
        boolean isAdmin = SecurityUtil.loggedInUserHasAccessScope(NomApiAccessScope.ADMIN);
        boolean canCreateResourcesInOrgenhet =  authorizedNavident.kanBrukerOppretteRessurs(navIdent, orgenhetId);
        log.info("hasNavidentAccessToResourcesForOrgenhet, isAdmin: {}, canCreateResourcesInOrgenhet: {}", isAdmin, canCreateResourcesInOrgenhet);

        List<IOrgenhet> editableOrgenheter = authorizedNavident.orgenheterLederKanRedigere(navIdent);
        boolean canEditResourcesInOrgenhet = editableOrgenheter.stream().anyMatch(orgenhet -> orgenhet.getId().equals(orgenhetId));
        log.info("hasNavidentAccessToResourcesForOrgenhet, isAdmin: {}, canEditResourcesInOrgenhet: {}", isAdmin, canEditResourcesInOrgenhet);

        return isAdmin || canCreateResourcesInOrgenhet;
    }

    @Operation(
        description = "om current bruker har tilgang til å opprette ressurser i angitt orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("isAuthorizedForOrgenhet/{orgenhetId}")
    @PreAuthorize("hasAuthority('NOM_UI_APP')")
    boolean isAuthorizedForOrgenhet(@PathVariable String orgenhetId) {
        return authResolver.kanOppretteRessurs(orgenhetId);
    }

    @GetMapping("getLederTilgangerDelegertTilMeg")
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    List<LederTilgangState> getLederTilgangerForUser() {
        return authorizedNavident.getLederTilgangerForUser();
    }

    @GetMapping("getLederTilgangerDelegertForOrgEnhet")
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    List<LederTilgangState> getLederTilgangerForOrgEnhet() {
        return authorizedNavident.getLederTilgangerForOrgenhet();
    }

    @GetMapping("getDelegerteLederTilgangerAvUser")
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    List<LederTilgangState> getLederTilgangGittAvBruker() {
        return authorizedNavident.getLederTilgangGittAvBruker();
    }

    @Operation(
        description = "Deleger tilgang til å redigere ressurs til en annen bruker",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("addDelegatedAuthorization")
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    void addDelegatedAuthorization(@Valid @RequestBody LederTilgangForOrgenhetRequest request) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        if (lederNavident.isEmpty()) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        authorizedNavident.addLederTilgangForOrgenhet(lederNavident.get(), request);
    }

    @Operation(
        description = "Fjern tilgang til å redigere ressurs for gitt bruker",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("removeDelegatedAuthorization/{navidentDelegertLeder}")
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    void removeDelegatedAuthorization(@PathVariable String navidentDelegertLeder, @RequestParam(name = "orgenhet") String orgenhetId) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        if (lederNavident.isEmpty()) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
        authorizedNavident.removeLederTilgangForOrgenhet(lederNavident.get(), navidentDelegertLeder, orgenhetId);
    }

    @Operation(
        description = "Forny tilgang til å redigere ressurs til en annen bruker",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("renewDelegatedAuthorization")
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    void renewDelegatedAuthorization(@RequestBody @Valid LederTilgangForOrgenhetRequest request) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        if (lederNavident.isEmpty()) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        authorizedNavident.renewLederTilgangForOrgenhet(lederNavident.get(), request);
    }

    @GetMapping("isAuthorizedToDelegate/{navident}")
    @Operation(
        description = "Returner en liste med orgenheter du har tilgang til å delegere på.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')")
    public List<IOrgenhet> isAuthorizedToDelegate(@PathVariable String navident) {
        return authorizedNavident.isAuthorizedToDelegate(navident);
    }

    @GetMapping("getRessurserWhichCanBeDelegatedTo")
    @Operation(
            description = "Returner en liste med navn og navIdent for ressurser som har AD-gruppe NOM-WRITE-DELEGERT og/eller NOM-WRITE-LEDER og dermed kan delegeres til.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('NOM_UI_APP')")
    public List<MinimalRessurs> getRessurserWhichCanBeDelegatedTo() {
        return delegationAdService.getRessurserWhichCanBeDelegatedTo();
    }

    @GetMapping("getOrgEnheterLederCanEdit")
    @Operation(
        description = "Returner en liste med orgenheter innlogget bruker har tilgang til å redigere ressurser i.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('NOM_UI_APP')")
    public List<IOrgenhet> getOrgEnheterLederCanEdit(@RequestParam(required = false) String navident, @RequestParam(required = false) String kuntoppnoder) {
        var kunToppNoder = kuntoppnoder != null && List.of("true", "True", "TRUE").contains(kuntoppnoder);
        var isProvidedNavident = navident != null && !navident.isBlank();
        // if navident is provided, check for that user, otherwise use the authenticated user
        log.info("getOrgEnheterLederCanEdit, " +
            "navident: {}, kunToppNoder: {}, isProvidedNavident: {}, getAuthenticatedNavIdent(): {}",
            navident, kunToppNoder, isProvidedNavident, getAuthenticatedNavIdent());
        return authorizedNavident.orgenheterLederKanRedigere(isProvidedNavident ? navident : getAuthenticatedNavIdent(), kunToppNoder);
    }

    @PostMapping("getLedereForOrgenheter")
    @Operation(
        description = "Returner et map fra gitte orgenhetIDer til tilhørende ressurser med redigeringstilgang.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('NOM_UI_APP')")
    public Map<String,List<MinimalRessurs>> getLedereMedRedigeringsTilgangForEnhet(@RequestBody List<String> orgenhetIdSet) {
        return authorizedNavident.ledereSomKanRedigereOrgenhet(Set.copyOf(orgenhetIdSet));
    }

    @ExceptionHandler(
        {
            AccessDeniedException.class,
        }
    )
    public ResponseEntity<StandardResponse> handleForbidden(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(
        {
            IllegalArgumentException.class
        }
    )
    public ResponseEntity<StandardResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(
        {
            NotAuthorizedToDelegateLeder.class
        }
    )
    public ResponseEntity<StandardResponse> handleNotAuthorizedToDelegateLeder(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }
}
