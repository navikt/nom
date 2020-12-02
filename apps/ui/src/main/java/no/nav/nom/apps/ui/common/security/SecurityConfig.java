package no.nav.nom.apps.ui.common.security;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.ui.azure.AadOAuth2AccessTokenResponseClient;
import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Set;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    public static final String ROTATION_ENDPOINT = "/api/v1/me/rotate-credentials";
    private final AadOAuth2AccessTokenResponseClient tokenResponseClient;
    private final CookieBasedAuthorizedClientRepository cookieBasedAuthorizedClientRepository;
    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;
    @Value("${app.security.resource-server.accepted-audience}")
    private Set<String> acceptedAudience;

    @Override
    public void configure(WebSecurity web) {
        /*
        Skru av Spring Security for alt under internal.
        Begrunnelse for å bruke dette i stede for å ta http.mvcMatchers("/internal/**").permitAll()
        er at dersom credentials har utgått, så vil de trigge en reauth selv på endepunkter som er ignorert.
         */
        web.ignoring().mvcMatchers("/internal/**", ROTATION_ENDPOINT);
    }

    /*
    Note: Per nå, så kjøres kun nom-ui som 1x node.
    Dersom man ønsker å kjøre den i flere noder, må man opprette en egen `oauth2Client().authorizedClientRepository(newStuff)` som er distribuert.
    Dette er for å holde context for mellom request, redirect til IDP (AzureAD) og tilbake (E.g. State key, nounce, o.l..).
    Grunnen til at man trenger en distribuert løsning blir for å støtte at bruker kan treffe en annen pod når han kommer tilbake igjen fra AzureAD.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                // Alle kall som ikke inngår i '/internal/**' må være autentisert.
                .anyRequest()
                .fullyAuthenticated()
                .and()

                .oauth2Login() // Login skjer via OAuth2
                .authorizedClientRepository(cookieBasedAuthorizedClientRepository) // Tilstand skal lagres tilbake i Cookies.
                .tokenEndpoint()
                .accessTokenResponseClient(tokenResponseClient) // Bytte fra code til AccessToken, IdToken + RefreshToken skjer via egen Azure AD client.
                .and()
                .and()

                .oauth2ResourceServer()
                .bearerTokenResolver(new CookieBasedTokenResolver()) // Tokens hentes fra cookies
                .jwt()
                .decoder(JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, acceptedAudience)) // Default implementasjon validerer ikke Audience, så overskrives.
                .and()
                .and()

                .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // Bruk Cookies for å holde på Cross-Site Request Forgery Tokens.
                .and();
    }
}
