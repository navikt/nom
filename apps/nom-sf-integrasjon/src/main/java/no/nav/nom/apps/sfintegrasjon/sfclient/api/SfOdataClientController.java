package no.nav.nom.apps.sfintegrasjon.sfclient.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfODataClient;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfOAuthService;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionService;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.EmpEmployment;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FOBusinessUnit;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODepartment;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODivision;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.PerPerson;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.User;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sf/client")
@Tag(name = "SF ODATA Client", description = "Endpoints for SAP SuccessFactors authentication. Admin only in dev/prod.")
public class SfOdataClientController {

    private final SamlAssertionService samlAssertionService;
    private final SfOAuthService sfOAuthService;
    private final SfODataClient sfODataClient;

    public SfOdataClientController(SamlAssertionService samlAssertionService, SfOAuthService sfOAuthService, SfODataClient sfODataClient) {
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

    @GetMapping(value = "/odata-metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch complete OData $metadata",
        description = "Fetches the complete OData v2 $metadata document for all entities from SAP SuccessFactors. Authenticates with a fresh OAuth token."
    )
    public ResponseEntity<String> getAllODataMetadata() {
        try {
            String metadata = sfODataClient.getAllMetadata();
            return ResponseEntity.ok(metadata);
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

    @GetMapping(value = "/odata-users", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch first 10 SF User objects",
        description = "Fetches the first 10 User entities from SAP SuccessFactors using the generated VDM client. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsers(
        @RequestParam(required = false) String accessToken
    ) {
        try {
            return ResponseEntity.ok(sfODataClient.getFirst10Users(accessToken).stream().map(User::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch users from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping(
        value = "/odata-users/by-ssn",
        consumes = MediaType.TEXT_PLAIN_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF User objects by SSN",
        description = "Fetches User records matching the SSN provided as plain text in the request body. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsersBySsn(
        @RequestBody String ssn,
        @RequestParam(required = false) String accessToken
    ) {
        if (ssn.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(sfODataClient.getUsersBySsn(ssn, accessToken).stream().map(User::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch users by SSN from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-users/by-emp-id", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF User objects by employee ID",
        description = "Fetches User records matching the given employee ID. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsersByEmpId(
        @RequestParam String empId,
        @RequestParam(required = false) String accessToken
    ) {
        if (empId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(sfODataClient.getUsersByEmpId(empId, accessToken).stream().map(User::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch users by employee ID from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-users/by-user-id", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF User objects by user ID",
        description = "Fetches User records matching the given user ID. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsersByUserId(
        @RequestParam String userId,
        @RequestParam(required = false) String accessToken
    ) {
        if (userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(sfODataClient.getUsersByUserId(userId, accessToken).stream().map(User::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch users by user ID from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-users/by-username", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF User objects by username",
        description = "Fetches User records matching the given username. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsersByUsername(
        @RequestParam String username,
        @RequestParam(required = false) String accessToken
    ) {
        if (username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(sfODataClient.getUsersByUsername(username, accessToken).stream().map(User::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch users by username from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-users/by-email", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF User objects by email",
        description = "Fetches User records matching the given email address. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsersByEmail(
        @RequestParam String email,
        @RequestParam(required = false) String accessToken
    ) {
        if (email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(sfODataClient.getUsersByEmail(email, accessToken).stream().map(User::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch users by email from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-users/by-display-name", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF User objects by display name",
        description = "Fetches User records matching the given display name. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataUsersByDisplayName(
        @RequestParam String displayName,
        @RequestParam(required = false) String accessToken
    ) {
        if (displayName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(
                sfODataClient.getUsersByDisplayName(displayName, accessToken).stream().map(User::toString).toList()
            );
        } catch (ODataException e) {
            log.error("Failed to fetch users by display name from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-emp-employment", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch first 10 SF EmpEmployment objects",
        description = "Fetches the first 10 EmpEmployment entities from SAP SuccessFactors using the generated VDM client. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataEmpEmployments(
        @RequestParam(required = false) String accessToken
    ) {
        try {
            return ResponseEntity.ok(sfODataClient.getFirst10EmpEmployments(accessToken).stream().map(EmpEmployment::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch EmpEmployment objects from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-per-person", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch first 10 SF PerPerson objects",
        description = "Fetches the first 10 PerPerson entities from SAP SuccessFactors using the generated VDM client. " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataPerPersons(
        @RequestParam(required = false) String accessToken
    ) {
        try {
            return ResponseEntity.ok(sfODataClient.getFirst10PerPersons(accessToken).stream().map(PerPerson::toString).toList());
        } catch (ODataException e) {
            log.error("Failed to fetch PerPerson objects from SAP SuccessFactors OData API", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-fo-department/{nomOrgEnhetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF FODepartment by NOM orgenhet-ID",
        description = "Fetches all FODepartment records matching the given NOM orgenhet-ID (mapped to externalCode in SF). " +
            "May return multiple records when the department has several time-bounded versions (historikk). " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataFODepartmentByNomOrgEnhetId(
        @PathVariable String nomOrgEnhetId,
        @RequestParam(required = false) String accessToken
    ) {
        try {
            return ResponseEntity.ok(
                sfODataClient.getFODepartmentsByNomOrgEnhetId(nomOrgEnhetId, accessToken)
                    .stream()
                    .map(FODepartment::toString)
                    .toList()
            );
        } catch (ODataException e) {
            log.error("Failed to fetch FODepartment for nomOrgEnhetId={} from SAP SuccessFactors OData API", nomOrgEnhetId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-fo-division/{nomOrgEnhetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF FODivision by NOM orgenhet-ID",
        description = "Fetches all FODivision records matching the given NOM orgenhet-ID (mapped to externalCode in SF). " +
            "May return multiple records when the division has several time-bounded versions (historikk). " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataFODivisionByNomOrgEnhetId(
        @PathVariable String nomOrgEnhetId,
        @RequestParam(required = false) String accessToken
    ) {
        try {
            return ResponseEntity.ok(
                sfODataClient.getFODivisionsByNomOrgEnhetId(nomOrgEnhetId, accessToken)
                    .stream()
                    .map(FODivision::toString)
                    .toList()
            );
        } catch (ODataException e) {
            log.error("Failed to fetch FODivision for nomOrgEnhetId={} from SAP SuccessFactors OData API", nomOrgEnhetId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/odata-fo-business-unit/{nomOrgEnhetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Fetch SF FOBusinessUnit by NOM orgenhet-ID",
        description = "Fetches all FOBusinessUnit records matching the given NOM orgenhet-ID (mapped to externalCode in SF). " +
            "May return multiple records when the business unit has several time-bounded versions (historikk). " +
            "Uses the cached SF OAuth token unless an accessToken query parameter is provided."
    )
    public ResponseEntity<List<String>> getODataFOBusinessUnitByNomOrgEnhetId(
        @PathVariable String nomOrgEnhetId,
        @RequestParam(required = false) String accessToken
    ) {
        try {
            return ResponseEntity.ok(
                sfODataClient.getFOBusinessUnitsByNomOrgEnhetId(nomOrgEnhetId, accessToken)
                    .stream()
                    .map(FOBusinessUnit::toString)
                    .toList()
            );
        } catch (ODataException e) {
            log.error("Failed to fetch FOBusinessUnit for nomOrgEnhetId={} from SAP SuccessFactors OData API", nomOrgEnhetId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    public record SamlAssertionResponse(String assertion) {}

    public record ValidateTokenRequest(String token) {}
}
