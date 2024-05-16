package no.nav.nom.apps.api.graphql.security;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;
import no.nav.nom.apps.api.common.security.AppGrantedAuthoritiesProperties;
import no.nav.nom.apps.api.common.security.JWTFilter;
import no.nav.nom.apps.api.common.security.SecurityConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.eventsourcing.metadata.RessursMetadataRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
//    WebsocketSourceConfiguration.class,
    SecurityUtil.class,
    SecurityConfig.class,
    JWTFilter.class,
    AZPAuthoritiesConverter.class,
//        NimbusJwtDecoder.class, // Note
//        DefaultJWTProcessor.class,
    GraphQlProperties.class
})
@EnableConfigurationProperties(value = AppGrantedAuthoritiesProperties.class)
public class SecurityTestConfig {
    @MockBean
    OrgenhetService orgenhetService;
    @MockBean
    Unleash unleash;
    @MockBean
    OrgtilknytningService orgtilknytningService;
    @MockBean
    RessursStateRepository ressursStateRepository;
    @MockBean
    DvhRepository dvhRepository;
    @MockBean
    OrganiseringStateRepository organiseringStateRepository;
    @MockBean
    RemedyEnhetMapperService remedyEnhetMapperService;
    @MockBean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;
    @MockBean
    RessursMetadataRepository ressursMetadataRepository;
    @MockBean
    LuceneService luceneService;
}
