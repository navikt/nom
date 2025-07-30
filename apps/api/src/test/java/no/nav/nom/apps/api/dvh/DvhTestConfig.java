package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsClientImpl;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.organisering.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
    DvhRestConsumer.class
    , DvhRepository.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , DvhOrdsService.class
    , DvhOrdsClientImpl.class
    , ValidationService.class
    , NomProxyClient.class
    , LeaderElection.class
    , ObjectMapper.class
    , OTUtenforHierarki.class
    , OrgtilknytningService.class
})
@EnableConfigurationProperties(value = AppProperties.class)
public class DvhTestConfig {
    @MockitoBean
    RessursStateRepository ressursStateRepository;
    @MockitoBean
    OrgenhetService orgenhetService;
    @MockitoBean
    OrgEnhetStateRepository orgEnhetStateRepository;
    @MockitoBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;
//    @MockitoBean
//    DvhOrdsClient dvHOrdsClient;
    @MockitoBean
    NomStateRessursListener nomStateRessursListener;
    @MockitoBean
    NomStateOrgEnhetListener nomStateOrgEnhetListener;
    @MockitoBean
    NomStateOrganiseringListener nomStateOrganiseringListener;
    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @MockitoBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @MockitoBean
    NomStateDelegertLederListener nomStateDelegertLederListener;
    @MockitoBean
    RessursHendelseForEventsourceService ressursHendelseForEventsourceService;
    @MockitoBean
    ValidationRepository validationRepository;
    @MockitoBean
    OrgEnhetCommandService orgEnhetCommandService;
    @MockitoBean
    OrgEnhetIdGenerator orgEnhetIdGenerator;
    @MockitoBean
    OrgEnhetTypeHandler orgEnhetTypeHandler;
    @MockitoBean
    RessursStillingLoader ressursStillingLoader;
    @MockitoBean
    EksterneRessurserLoader eksterneRessurserLoader;
    @MockitoBean
    OrganiseringService organiseringService;
    @MockitoBean
    KommunaleRessurserLoader kommunaleRessurserLoader;
    @MockitoBean
    AuthorizedNavident authorizedNavident;
    @MockitoBean
    DvhStartupLoadMonitor dvhStartupLoadMonitor;
    @MockitoBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    @MockitoBean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;
}
