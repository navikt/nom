package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateListener;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Import({
    DvhRestConsumer.class
    , DvhRepository.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , ValidationService.class
    , NomProxyClient.class
    , LeaderElection.class
    , ObjectMapper.class
})
@EnableConfigurationProperties(value = AppProperties.class)
public class DvhTestConfig {
    @MockBean
    RessursStateRepository ressursStateRepository;
    @MockBean
    OrgenhetService orgenhetService;
    @MockBean
    OrgEnhetStateRepository orgEnhetStateRepository;
    @MockBean
    NomProxyRestClient nomProxyRestClient;
    @MockBean
    NomStateRessursListener nomStateRessursListener;
    @MockBean
    NomStateOrgEnhetListener nomStateOrgEnhetListener;
    @MockBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockBean
    OrgtilknytningStateListener orgtilknytningStateListener;
    @MockBean
    OrgEnhetStateListener orgEnhetStateListener;
    @MockBean
    RessursHendelseForEventsourceService ressursHendelseForEventsourceService;
    @MockBean
    ValidationRepository validationRepository;
    @MockBean
    OrgEnhetCommandService orgEnhetCommandService;
    @MockBean
    OrgEnhetIdGenerator orgEnhetIdGenerator;
    @MockBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;
    @MockBean
    RessursStillingLoader ressursStillingLoader;
    @MockBean
    EksterneRessurserLoader eksterneRessurserLoader;
    @MockBean
    OrganiseringService organiseringService;
    @MockBean
    KommunaleRessurserLoader kommunaleRessurserLoader;
    @MockBean
    AuthorizedNavident authorizedNavident;
    @MockBean
    DvhStartupLoadMonitor dvhStartupLoadMonitor;
    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    @MockBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;
}
