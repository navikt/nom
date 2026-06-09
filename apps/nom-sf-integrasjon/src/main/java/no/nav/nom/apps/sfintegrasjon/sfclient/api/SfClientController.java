package no.nav.nom.apps.sfintegrasjon.sfclient.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfODataClient;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfOAuthService;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;

/**
 * Debug endpoints for inspecting SAP SuccessFactors authentication artifacts.
 * Accessible locally without auth; requires ADMIN group membership in dev/prod.
 */
@RestController
@RequestMapping("/api/sf/client")
@Tag(name = "SF Client", description = "Endpoints for SAP SuccessFactors authentication. Admin only in dev/prod.")
public class SfClientController {

    private final SamlAssertionService samlAssertionService;
    private final SfOAuthService sfOAuthService;
    private final SfODataClient sfODataClient;

    public SfClientController(SamlAssertionService samlAssertionService, SfOAuthService sfOAuthService, SfODataClient sfODataClient) {
        this.samlAssertionService = samlAssertionService;
        this.sfOAuthService = sfOAuthService;
        this.sfODataClient = sfODataClient;
    }

    @PostMapping("/saml-assertion")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Generate SAML assertion",
        description = "Generates a signed SAML 2.0 Bearer assertion. Optional body overrides specific fields; omit fields to use configured defaults."
    )
    public ResponseEntity<SamlAssertionResponse> generateSamlAssertion(
        @RequestBody(required = false) SamlAssertionParams params
    ) {
        SamlAssertionParams effectiveParams = params != null ? params : SamlAssertionParams.defaults();
        String assertion = samlAssertionService.generateAssertion(effectiveParams);
        return ResponseEntity.ok(new SamlAssertionResponse(assertion));
    }

    @PostMapping(value = "/saml-assertion-xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Generate SAML assertion (decoded XML)",
        description = "Same as /saml-assertion but returns the raw XML instead of base64 — useful for inspecting the assertion structure."
    )
    public ResponseEntity<String> generateSamlAssertionXml(
        @RequestBody(required = false) SamlAssertionParams params
    ) {
        SamlAssertionParams effectiveParams = params != null ? params : SamlAssertionParams.defaults();
        String assertion = samlAssertionService.generateAssertion(effectiveParams);
        String xml = new String(Base64.getDecoder().decode(assertion));
        return ResponseEntity.ok(xml);
    }

    @PostMapping("/oauth-token")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Generate OAuth access token",
        description = "Generates a SAML assertion and exchanges it for an SF OAuth 2.0 access token. Optional body overrides specific fields."
    )
    public ResponseEntity<SfAccessToken> generateOAuthToken(
        @RequestBody(required = false) SamlAssertionParams params
    ) {
        SamlAssertionParams effectiveParams = params != null ? params : SamlAssertionParams.defaults();
        SfAccessToken token = sfOAuthService.generateToken(effectiveParams);
        return ResponseEntity.ok(token);
    }

    @PostMapping(value = "/validate-token", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Validate OAuth access token",
        description = "Validates an SF OAuth 2.0 access token via the SAP SuccessFactors /oauth/validate endpoint. " +
            "Returns the raw response body from SAP. Returns the upstream HTTP status if SAP rejects the token."
    )
    public ResponseEntity<String> validateToken(@RequestBody ValidateTokenRequest request) {
        if (request.token() == null || request.token().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String rawBody = sfOAuthService.validateToken(request.token());
            return ResponseEntity.ok(rawBody);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/odata-metadata/{entity}", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch OData $metadata for an SF entity",
        description = "Fetches the OData v2 $metadata document for the given entity from SAP SuccessFactors. " +
            "Entity name must contain only letters (a-z, A-Z). Authenticates with a fresh OAuth token."
    )
    public ResponseEntity<String> getODataMetadata(@PathVariable String entity) {
        if (!entity.matches("[a-zA-Z]+")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String metadata = sfODataClient.getEntityMetadata(entity);
            return ResponseEntity.ok(metadata);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    public record SamlAssertionResponse(String assertion) {}

    public record ValidateTokenRequest(String token) {}
}
