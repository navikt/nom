package no.nav.nom.apps.api.authorize.redigereressurs.api;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.authorize.redigereressurs.api.app.AuthorizedNavidentController;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;
import no.nav.nom.apps.api.common.security.AppGrantedAuthoritiesProperties;
import no.nav.nom.apps.api.common.security.JWTFilter;
import no.nav.nom.apps.api.common.security.SecurityConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Import({
    AuthorizedNavidentController.class,
    AuthorizedNavident.class,
    AuthResolver.class,
    SecurityConfig.class,
    JWTFilter.class,
    AZPAuthoritiesConverter.class,
})
@EnableConfigurationProperties({AppGrantedAuthoritiesProperties.class, AppProperties.class})
public class TestConfig  {}
