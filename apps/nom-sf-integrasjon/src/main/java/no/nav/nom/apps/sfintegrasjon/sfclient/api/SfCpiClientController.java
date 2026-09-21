package no.nav.nom.apps.sfintegrasjon.sfclient.api;

import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfODataClient;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfOAuthService;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionService;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SfCpiClient;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.EmpEmployment;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.PerPerson;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(name = "SF CPI Client", description = "Endpoints for SAP SuccessFactors authentication. Admin only in dev/prod.")
public class SfCpiClientController {

    private final SfCpiClient sfCpiClient;

    public SfCpiClientController(SfCpiClient sfCpiClient) {
        this.sfCpiClient = sfCpiClient;
    }

    @PostMapping("/postDummyOrgenhetData")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Post a dummy Orgenhet data payload to SF CPI"
    )
    public ResponseEntity<String> postDummyOrgenhetData(
        @RequestBody(required = false) String payload
    ) {
        if (payload == null) {
            payload = """
                      {
                          "sporingsId": "KUN FOR ENDEPUNKT TESTING",
                          "endretAv": ["A990001"],
                          "endringshendelser": ["EndepunktsTest"],
                          "orgenhet": {
                              "id": "ab123c",
                              "navn": "Test",
                              "nivaa": "DRIFTSENHET",
                              "type": "DIR",
                              "tilgangsenhet": "1234",
                              "kostnadssted": "0123400",
                              "gyldigFom": "2026-11-01",
                              "ledere": [
                                  {
                                      "navident": "A990001",
                                      "gyldigFom": "2026-11-01"
                                  }
                              ],
                      
                              "overenheter": [
                                  {
                                      "id": "cb321a",
                      		         "nivaa": "DRIFTSENHET",
                                      "gyldigFom": "2026-11-01"
                                  }
                              ]
                          }
                      """;
        }
        try {
            return sfCpiClient.sendOrgenhetPayload(payload);
        } catch (RestClientResponseException e) {
            log.error(
                "SAP CPI returned error status {} for postDummyOrgenhetData. Body: {}",
                e.getStatusCode(),
                e.getResponseBodyAsString(),
                e
            );
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
