package no.nav.nom.apps.api.authorize.redigereressurs.api;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;
import no.nav.nom.apps.api.common.security.AppGrantedAuthoritiesProperties;
import no.nav.nom.apps.api.common.security.JWTFilter;
import no.nav.nom.apps.api.common.security.SecurityConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
    AuthorizedNavidentController.class,
    AuthorizedNavident.class,
    AuthResolver.class,
    SecurityConfig.class,
    JWTFilter.class,
    AZPAuthoritiesConverter.class,
})
@EnableConfigurationProperties(value = AppGrantedAuthoritiesProperties.class)
public class TestConfig  {
    @MockBean
    Unleash unleash;
}
