package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhLegacyRessursUtils;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.pdlutils.PdlUtils;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasErrors;
import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class GraphQLTestBase extends TestBase {

    @Autowired
    protected GraphQLTestTemplate graphQLTestTemplate;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected PdlUtils pdlUtils;

    @Autowired
    protected DvhLegacyRessursUtils dvhLegacyRessursUtils;

    @Autowired
    protected IRessursService ressursService;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
        pdlUtils.reset();
    }

    @SneakyThrows
    protected GraphQLResponse postForResource(String resource, boolean hasErrors) {
        return postForResource(resource, hasErrors, Map.of());
    }

    @SneakyThrows
    protected GraphQLResponse postForResource(String resource, boolean hasErrors, Map<String, Object> vars) {
        val claims = validJwtWithAzpClaim();
        claims.claim("azp_name","test");
        claims.claim("NAVident", "A321321");
        val token = securityTestUtils.createToken(claims);

        val varNode = (ObjectNode) om.readTree(om.writeValueAsString(vars));

        val response = graphQLTestTemplate.withBearerAuth(token).perform(resource, varNode);
        if (!hasErrors) {
            assertThat(response, hasNoErrors());
        } else {
            assertThat(response, hasErrors());
        }
        return response;
    }

    protected RessursI createRessursHendelse(String suffix) {
        val metadata = HendelseMetadataSerialize.builder()
                .opprettetAv(new HendelseMetadata.OpprettetAv("test","test"))
                .rawInputData(om.createObjectNode().put("suffix",suffix))
                .opprettetTidspunkt(LocalDateTime.now())
                .build();
        return ressursService.opprettRessurs("testPersonIdent" + suffix, "testNavIdent" + suffix, null, metadata);
    }
}
