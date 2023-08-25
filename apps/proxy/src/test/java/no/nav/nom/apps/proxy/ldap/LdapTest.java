package no.nav.nom.apps.proxy.ldap;

import no.nav.nom.apps.proxy.api.ApiTestBase;
import no.nav.nom.contract.ad.AdRessurs.AdStatus;
import no.nav.nom.contract.ad.AdRessurser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.List;

import static no.nav.nom.libs.utils.common.util.StreamUtils.find;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapTest extends ApiTestBase {

    private static final String AD_URL = "/proxy/ad";

    @Autowired
    OnpremAdConsumer consumer;

    @Test
    void getAdRessurs() {

        webTestClient
            .post()
            .uri(AD_URL)
            .body(Mono.just(List.of("A123456", "A123457", "A123458")), List.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(securityTestUtils.getValidToken()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(AdRessurser.class)
            .consumeWith(result -> {
                assertThat(result.getResponseBody()).isNotNull();
                assertThat(result.getResponseBody().getRessurser()).hasSize(3);

                var john = find(result.getResponseBody().getRessurser(), r -> r.getNavIdent().equals("A123456"));
                assertThat(john.getEmail()).isEqualTo("john.doe@adeo.no");
                assertThat(john.getDisplayName()).isEqualTo("John Doe");
                assertThat(john.getStatus()).isEqualTo(AdStatus.OK);

                var jane = find(result.getResponseBody().getRessurser(), r -> r.getNavIdent().equals("A123457"));
                assertThat(jane.getEmail()).isEqualTo("jane.doe@adeo.no");
                assertThat(jane.getStatus()).isEqualTo(AdStatus.OK);

                var notFound = find(result.getResponseBody().getRessurser(), r -> r.getNavIdent().equals("A123458"));
                assertThat(notFound.getStatus()).isEqualTo(AdStatus.IKKE_FUNNET);
            });
    }
}
