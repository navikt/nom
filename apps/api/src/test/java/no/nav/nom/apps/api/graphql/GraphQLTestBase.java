package no.nav.nom.apps.api.graphql;

import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.pdlutils.PdlUtils;
import no.nav.nom.contract.dvh.Ressurs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasErrors;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class GraphQLTestBase extends TestBase {
    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    protected GraphQLTestTemplate graphQLTestTemplate;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    private RSAKey rsaKey;

    @Autowired
    protected PdlUtils pdlUtils;

    @BeforeEach
    public void setup() {
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", rsaKey.toPublicJWK().toString()))));
    }

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
        pdlUtils.reset();
    }

    protected Ressurs ressursTestTemplate(String suffix) {
        return Ressurs.builder()
                .navident("testNavIdent" + suffix)
                .personIdent("testPersonIdent" + suffix)
                .build();
    }

    @SneakyThrows
    protected GraphQLResponse postForResource(String resource, boolean hasErrors) {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val response = graphQLTestTemplate.withBearerAuth(token).postForResource(resource);
        if (!hasErrors) {
            assertThat(response, hasNoErrors());
        } else {
            assertThat(response, hasErrors());
        }
        return response;
    }

}
