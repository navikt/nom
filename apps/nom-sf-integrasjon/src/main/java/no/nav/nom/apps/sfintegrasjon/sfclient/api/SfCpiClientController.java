package no.nav.nom.apps.sfintegrasjon.sfclient.api;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfCpiClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

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
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Optional payload data",
            content = @Content(schema = @Schema(type = "string", example = ""))
        )
        @RequestBody(required = false) String payload
    ) {
        if (Strings.isNullOrEmpty(payload)) {
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
        log.info("postDummyOrgenhetData: sending payload to SAP CPI");
        try {
            String responseBody = sfCpiClient.sendOrgenhetPayload(payload);
            log.info("postDummyOrgenhetData: SAP CPI responded successfully");
            return ResponseEntity.ok(responseBody);
        } catch (RestClientResponseException e) {
            log.error(
                "postDummyOrgenhetData: SAP CPI returned error status {}. Body: {}",
                e.getStatusCode(),
                e.getResponseBodyAsString(),
                e
            );
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("postDummyOrgenhetData: call to SAP CPI failed with {}", e.getClass().getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Call to SAP CPI failed: " + e.getMessage());
        }
    }
}
