package no.nav.nom.apps.api.dvh;

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
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.bean.override.mockito.MockReset;
import tools.jackson.databind.json.JsonMapper;

@Import({
    DvhRestConsumer.class
    , DvhRepository.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , DvhOrdsService.class
    , DvhOrdsClientImpl.class
    , WebClientAutoConfiguration.class
    , ValidationService.class
    , NomProxyClient.class
    , LeaderElection.class
    , JsonMapper.class
    , OTUtenforHierarki.class
    , OrgtilknytningService.class
})
@EnableConfigurationProperties(value = AppProperties.class)
public class DvhTestConfig {
    @Bean
    RessursStateRepository ressursStateRepository() {
        return Mockito.mock(RessursStateRepository.class, MockReset.after());
    }
    @Bean
    OrgenhetService orgenhetService() {
        return Mockito.mock(OrgenhetService.class, MockReset.after());
    }
    @Bean
    OrgEnhetStateRepository orgEnhetStateRepository() {
        return Mockito.mock(OrgEnhetStateRepository.class, MockReset.after());
    }
    @Bean
    OrgtilknytningStateRepository orgtilknytningStateRepository() {
        return Mockito.mock(OrgtilknytningStateRepository.class, MockReset.after());
    }
//    @Bean
//    DvhOrdsClient dvHOrdsClient() {
//        return Mockito.mock(DvhOrdsClient.class, MockReset.after());
//    }
    @Bean
    NomStateRessursListener nomStateRessursListener() {
        return Mockito.mock(NomStateRessursListener.class, MockReset.after());
    }
    @Bean
    NomStateOrgEnhetListener nomStateOrgEnhetListener() {
        return Mockito.mock(NomStateOrgEnhetListener.class, MockReset.after());
    }
    @Bean
    NomStateOrganiseringListener nomStateOrganiseringListener() {
        return Mockito.mock(NomStateOrganiseringListener.class, MockReset.after());
    }
    @Bean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener() {
        return Mockito.mock(NomStateOrgtilknytningListener.class, MockReset.after());
    }
    @Bean
    NomStateMasteredRessursListener nomStateMasteredRessursListener() {
        return Mockito.mock(NomStateMasteredRessursListener.class, MockReset.after());
    }
    @Bean
    NomStateDelegertLederListener nomStateDelegertLederListener() {
        return Mockito.mock(NomStateDelegertLederListener.class, MockReset.after());
    }
    @Bean
    NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener() {
        return Mockito.mock(NomStateOrgEnhetLederListener.class, MockReset.after());
    }
    @Bean
    RessursHendelseForEventsourceService ressursHendelseForEventsourceService() {
        return Mockito.mock(RessursHendelseForEventsourceService.class, MockReset.after());
    }
    @Bean
    ValidationRepository validationRepository() {
        return Mockito.mock(ValidationRepository.class, MockReset.after());
    }
    @Bean
    OrgEnhetCommandService orgEnhetCommandService() {
        return Mockito.mock(OrgEnhetCommandService.class, MockReset.after());
    }
    @Bean
    OrgEnhetIdGenerator orgEnhetIdGenerator() {
        return Mockito.mock(OrgEnhetIdGenerator.class, MockReset.after());
    }
    @Bean
    OrgEnhetTypeHandler orgEnhetTypeHandler() {
        return Mockito.mock(OrgEnhetTypeHandler.class, MockReset.after());
    }
    @Bean
    RessursStillingLoader ressursStillingLoader() {
        return Mockito.mock(RessursStillingLoader.class, MockReset.after());
    }
    @Bean
    EksterneRessurserLoader eksterneRessurserLoader() {
        return Mockito.mock(EksterneRessurserLoader.class, MockReset.after());
    }
    @Bean
    OrganiseringService organiseringService() {
        return Mockito.mock(OrganiseringService.class, MockReset.after());
    }
    @Bean
    AuthorizedNavident authorizedNavident() {
        return Mockito.mock(AuthorizedNavident.class, MockReset.after());
    }
    @Bean
    DvhStartupLoadMonitor dvhStartupLoadMonitor() {
        return Mockito.mock(DvhStartupLoadMonitor.class, MockReset.after());
    }
    @Bean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager() {
        return Mockito.mock(OAuth2AuthorizedClientManager.class, MockReset.after());
    }
    @Bean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState() {
        return Mockito.mock(ManuelleRessurserOgLedereTilState.class, MockReset.after());
    }
}
