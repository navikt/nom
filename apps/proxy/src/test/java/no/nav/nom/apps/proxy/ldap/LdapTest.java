package no.nav.nom.apps.proxy.ldap;

import lombok.val;
import no.nav.nom.apps.proxy.api.ApiTestBase;
import no.nav.nom.contract.ad.AdRessurs.AdStatus;
import no.nav.nom.contract.ad.AdRessurser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

import static no.nav.nom.libs.utils.common.util.StreamUtils.find;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapTest extends ApiTestBase {

    private static final String AD_URL = "/proxy/ad";

    @Autowired
    OnpremAdConsumer consumer;

    @Test
    void getAdRessurs() {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(AD_URL), HttpMethod.POST,
                new HttpEntity<>(List.of("A123456", "A123457", "A123458"), headers), AdRessurser.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getRessurser()).hasSize(3);

        var john = find(result.getBody().getRessurser(), r -> r.getNavIdent().equals("A123456"));
        assertThat(john.getEmail()).isEqualTo("john.doe@adeo.no");
        assertThat(john.getDisplayName()).isEqualTo("John Doe");
        assertThat(john.getStatus()).isEqualTo(AdStatus.OK);
        var jane = find(result.getBody().getRessurser(), r -> r.getNavIdent().equals("A123457"));
        assertThat(jane.getEmail()).isEqualTo("jane.doe@adeo.no");
        assertThat(jane.getStatus()).isEqualTo(AdStatus.OK);

        var notFound = find(result.getBody().getRessurser(), r -> r.getNavIdent().equals("A123458"));
        assertThat(notFound.getStatus()).isEqualTo(AdStatus.IKKE_FUNNET);
    }
}
