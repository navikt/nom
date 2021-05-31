package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.pdlutils.PdlUtils;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import no.nav.nom.contract.nom.hendelse.HendelseTo.Endringstype;
import no.nav.nom.contract.nom.hendelse.Hendelsestyper;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasErrors;
import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class GraphQLTestBase extends TestBase {

    @Autowired
    protected GraphQLTestTemplate graphQLTestTemplate;

    @Autowired
    private DvhRepository dvhRepository;
    @Autowired
    protected RessursRepository ressursRepository;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected PdlUtils pdlUtils;

    @AfterEach
    protected void clearRepository() {
        dvhRepository.clearRepository();
        ressursRepository.clearRepository();
        pdlUtils.reset();
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

    protected HendelseTo createRessursHendelse(String suffix) {
        return createRessursHendelse(UUID.randomUUID().toString(), "testNavIdent" + suffix, "testPersonIdent" + suffix);
    }

    protected HendelseTo createRessursHendelse(String nomId, String navIdent, String fnr) {
        return createHendelse(Map.of(
                "fid", nomId,
                "navIdent", navIdent,
                "personIdent", fnr
        ), Endringstype.OPPRETTET, Hendelsestyper.RESSURS_V1);
    }

    protected HendelseTo createHendelse(Map<String, Object> data, Endringstype endringstype, Hendelsestyper hendelsestyper) {
        return HendelseTo.builder()
                .id(UUID.randomUUID())
                .reqId(UUID.randomUUID())
                .type(hendelsestyper.getKey())
                .data(om.convertValue(data, ObjectNode.class))
                .opprettet(LocalDateTime.now())
                .endringstype(endringstype)
                .build();
    }

    protected String getPersonIdent(HendelseTo hendelse) {
        return hendelse.getData().get("personIdent").asText();
    }

    protected String getNavIdent(HendelseTo hendelse) {
        return hendelse.getData().get("navIdent").asText();
    }
}
