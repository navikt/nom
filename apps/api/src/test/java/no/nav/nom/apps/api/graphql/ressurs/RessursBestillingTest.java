package no.nav.nom.apps.api.graphql.ressurs;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.api.graphql.BestillingTestBase;
import no.nav.nom.apps.api.graphql.request.dto.BestillingMetadata.Aktivering;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.libs.test.RsaStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ressurs bestilling")
public class RessursBestillingTest extends BestillingTestBase {

    @Nested
    @WireMockStubs(RsaStub.class)
    class Opprett {

        @Test
        @DisplayName("Default verdier")
        void sce1() {
            var response = postForResource("bestilling/opprettressurs.graphql", false, Map.of());

            assertThat(response.get("$.data.opprettRessurs.nomId")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.navIdent")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestId")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestSetId")).isNotNull();
        }

        @Test
        @DisplayName("Default immediate")
        void sce2() {
            var response = postForResource("bestilling/opprettressurs.graphql", false, Map.of("aktivering", Aktivering.IMMEDIATE));

            assertThat(response.get("$.data.opprettRessurs.nomId")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.navIdent")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestId")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestSetId")).isNotNull();
        }

        @Test
        @DisplayName("Default for approval")
        void sce3() {
            var response = postForResource("bestilling/opprettressurs.graphql", false, Map.of("aktivering", Aktivering.FOR_APPROVAL));

            assertThat(response.get("$.data.opprettRessurs.nomId")).isNull();
            assertThat(response.get("$.data.opprettRessurs.navIdent")).isNull();
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestId")).isNotNull();
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestSetId")).isNotNull();
        }

        @Test
        @DisplayName("Chain 2")
        void sce4() {
            var response = postForResource("bestilling/opprettressurs.graphql", false, Map.of("aktivering", Aktivering.FOR_APPROVAL));
            assertThat(response.get("$.data.opprettRessurs.bestilling.requestId")).isNotNull();
            var requestSetId = response.get("$.data.opprettRessurs.bestilling.requestSetId");

            var response2 = postForResource("bestilling/opprettressurs.graphql", false, Map.of("aktivering", Aktivering.FOR_APPROVAL, "requestSetId", requestSetId));
            assertThat(response2.get("$.data.opprettRessurs.bestilling.requestId")).isNotNull();
            assertThat(response2.get("$.data.opprettRessurs.bestilling.requestSetId")).isEqualTo(requestSetId);
        }
    }

    @Test
    @DisplayName("Aktiver")
    void aktiver() {
        var response = postForResource("bestilling/opprettressurs.graphql", false, Map.of("aktivering", Aktivering.FOR_APPROVAL));
        var requestSetId = response.get("$.data.opprettRessurs.bestilling.requestSetId");
        var response2 = postForResource("bestilling/aktiver.graphql", false, Map.of("requestSetId", requestSetId));
        assertThat(response2.get("$.data.activateRequestSet")).isEqualTo(ResultCode.OK.name());
    }
}
