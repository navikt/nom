package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.GraphQlSupportImpl;
import no.nav.nom.apps.api.graphql.GraphQlTestBase;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.propertyresolvers.RessursPropertyResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils;
import no.nav.nom.apps.gql.GraphQlAutoConfiguration;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("GraphQLTest")
@GraphQlTest
public class OrgEnhTest extends GraphQlTestBase {


    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter")
    void scenario11() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().fid("FID_1").id("ID_1").agressoId("testId1").agressoOrgNivaa("ORGENHET").build();
        orgEnhetStateRepository.update(orgenhet);

        var outputEnhet = gqlUtil.postForResource("organisasjonsenhet/scenario1.graphql")
            .path("organisasjonsenhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(orgenhet.navn(), outputEnhet.getNavn());
        assertEquals(orgenhet.agressoId(), outputEnhet.getAgressoId());
        assertEquals(orgenhet.agressoOrgenhetType(), outputEnhet.getAgressoOrgenhetType());

        var res = gqlUtil.postForResource("organisasjonsenhet/scenario13.graphql");

        outputEnhet = res.path("orgEnhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(orgenhet.id(),outputEnhet.getId());
        assertEquals(orgenhet.navn(), outputEnhet.getNavn());
    }

}
