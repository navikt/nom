package no.nav.nom.apps.dvh.common;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.HashSet;
import java.util.List;


@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WebSecurityConfigAdapter extends WebSecurityConfigurerAdapter {

    private final Environment environment;
    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    @Override
    public void configure(WebSecurity web) {
        /*
        Skru av Spring Security for alt under internal.
        Begrunnelse for å bruke dette i stede for å ta http.mvcMatchers("/internal/**").permitAll()
        er at dersom credentials har utgått, så vil de trigge en reauth selv på endepunkter som er ignorert.
         */
        web.ignoring().mvcMatchers("/internal/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        val audience = new HashSet<String>(List.of(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.accepted-audience", String[].class)));

        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable()
                .authorizeRequests()
                .anyRequest()
                .fullyAuthenticated()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .decoder(JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, audience));
    }
}
