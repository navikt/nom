package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.domain.Ressurs;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.pdlutils.PdlUtils;
import no.nav.nom.apps.api.repository.Hendelse;
import no.nav.nom.apps.api.repository.Hendelse.HendelseType;
import no.nav.nom.apps.api.repository.Hendelse.DataType;
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
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected PdlUtils pdlUtils;

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
        val claims = securityTestUtils.validJwtClaims();
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

    protected Hendelse createRessursHendelse(String suffix) {
        return createRessursHendelse(UUID.randomUUID(), "testNavIdent" + suffix, "testPersonIdent" + suffix);
    }

    protected Hendelse createRessursHendelse(UUID fid, String navIdent, String fnr) {
        return createHendelse(Map.of(
                Ressurs.NAVIDENT_FIELD, navIdent,
                Ressurs.PERSONIDENT_FIELD, fnr
        ), HendelseType.OPPRETTET, DataType.RESSURS_V1, fid);
    }

    protected Hendelse createHendelse(Map<String, Object> data, HendelseType hendelseType, DataType dataType, UUID fid) {
        return Hendelse.builder()
                .hendelseID(UUID.randomUUID())
                .funksjonellId(fid)
                .foresporselId(UUID.randomUUID())
                .tidligereId(null)
                .dataType(dataType)
                .hendelseType(hendelseType)
                .data(om.convertValue(data, ObjectNode.class))
                .opprettetTidspunkt(LocalDateTime.now())
                .build();
    }

    protected String getPersonIdent(Hendelse hendelse) {
        return hendelse.getData().get(Ressurs.PERSONIDENT_FIELD).asText();
    }

    protected String getNavIdent(Hendelse hendelse) {
        return hendelse.getData().get(Ressurs.NAVIDENT_FIELD).asText();
    }
}
