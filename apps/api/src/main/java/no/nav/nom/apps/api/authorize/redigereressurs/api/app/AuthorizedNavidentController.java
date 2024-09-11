package no.nav.nom.apps.api.authorize.redigereressurs.api.app;


import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.NotAuthorizedToDelegateLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.rest.common.StandardResponse;
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

import java.util.List;
import java.util.Optional;

@Tag(name = "Controller metode for å verifisere redigere ressurs tilgang og administrere delegerering av tilganger")
@RestController
@Slf4j
@RequestMapping("/app/authorizedRessursRedigering")
public class AuthorizedNavidentController {

    private final AuthorizedNavident authorizedNavident;
    private final AuthResolver authResolver;
    private final Unleash unleash;
    public static final String UNLEASH_DELEGER_TILGANG = "nom.deleger-tilgang";

    public AuthorizedNavidentController(AuthorizedNavident authorizedNavident, AuthResolver authResolver, Unleash unleash) {
        this.authorizedNavident = authorizedNavident;
        this.authResolver = authResolver;
        this.unleash = unleash;
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
        if (!unleash.isEnabled(UNLEASH_DELEGER_TILGANG)) {
            throw new AccessDeniedException("Metode er feature togglet av");
        }

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
        if (!unleash.isEnabled(UNLEASH_DELEGER_TILGANG)) {
            throw new AccessDeniedException("Metode er feature togglet av");
        }

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
        if (!unleash.isEnabled(UNLEASH_DELEGER_TILGANG)) {
            throw new AccessDeniedException("Metode er feature togglet av");
        }

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
