package no.nav.nom.apps.api.graphql.ressurs;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.api.graphql.OrdreLinjeTestBase;
import no.nav.nom.libs.test.RsaStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ressurs bestilling")
public class RessursOrdreLinjeTest extends OrdreLinjeTestBase {

    @Nested
    @WireMockStubs(RsaStub.class)
    class Opprett {

        @Test
        @DisplayName("Default verdier")
        void sce1() {
            //var response = postForResource("ordre/ordreRessurs.graphql", false, Map.of());

            //assertThat(response.get("$.data.ordreRessurs.ordreLinje.ordreLinjeId")).isNotNull();
            //assertThat(response.get("$.data.ordreRessurs.ordreLinje.ordreId")).isNotNull();
        }
    }

    @Test
    @DisplayName("Godkjenn")
    void aktiver() {
        //var response = postForResource("ordre/ordreRessurs.graphql", false, Map.of());
        //var ordreId = response.get("$.data.ordreRessurs.ordreLinje.ordreId");
        //var response2 = postForResource("bestilling/aktiver.graphql", false, Map.of("ordreId", ordreId));
        //assertThat(response2.get("$.data.activateBestillingSet")).isEqualTo(ResultCode.OK.name());
    }
}
