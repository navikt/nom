package no.nav.nom.apps.api.authorize.redigereressurs.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Controller metode for å verifisere redigere ressurs tilgang og administrere delegerering av tilganger")
@RestController
@Slf4j
@RequestMapping("/api/app/authorizedRessursRedigering")
@PreAuthorize("hasAuthority('NOM_WRITE')")
public class AuthorizedNavidentController {

    private final AuthorizedNavident authorizedNavident;
    private final AuthResolver authResolver;

    public AuthorizedNavidentController(AuthorizedNavident authorizedNavident, AuthResolver authResolver) {
        this.authorizedNavident = authorizedNavident;
        this.authResolver = authResolver;
    }

    @Operation(
        description = "om current bruker har tilgang til å redigere ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("isAuthorizedFor/{navident}")
    void isAuthorizedFor(@PathVariable String navident) {
        if (!authResolver.kanRedigereRessurs(navident)) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
    }

    @Operation(
        description = "Deleger tilgang til å redigere ressurs til en annen bruker",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("addDelegatedAuthorization/{navidentDelegertLeder}")
    void addDelegatedAuthorization(@PathVariable String navidentDelegertLeder, @RequestParam(name = "orgenhet") String orgenhetId) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        if(lederNavident.isEmpty()) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
        authorizedNavident.addDelegertLederForOrgenhet(lederNavident.get(), navidentDelegertLeder, orgenhetId);
    }

    @Operation(
        description = "Deleger tilgang til å redigere ressurs til en annen bruker",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("removeDelegatedAuthorization/{navidentDelegertLeder}")
    void removeDelegatedAuthorization(@PathVariable String navidentDelegertLeder, @RequestParam(name = "orgenhet") String orgenhetId) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        if(lederNavident.isEmpty()) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
        authorizedNavident.removeDelegertLederForOrgenhet(lederNavident.get(), navidentDelegertLeder, orgenhetId);
    }

    @Operation(
        description = "Deleger tilgang til å redigere ressurs til en annen bruker",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("renewDelegatedAuthorization/{navidentDelegertLeder}")
    void renewDelegatedAuthorization(@PathVariable String navidentDelegertLeder, @RequestParam(name = "orgenhet") String orgenhetId) {
        Optional<String> lederNavident = SecurityUtil.getNavIdent(SecurityContextHolder.getContext().getAuthentication());

        if(lederNavident.isEmpty()) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
        authorizedNavident.renewDelegertLederForOrgenhet(lederNavident.get(), navidentDelegertLeder, orgenhetId);
    }

    @ExceptionHandler({
        AccessDeniedException.class,
    })
    public ResponseEntity<StandardResponse> handleForbidden(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler({
        IllegalArgumentException.class
    })
    public ResponseEntity<StandardResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }
}
