package no.nav.nom.apps.navident.common.security;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.HashSet;
import java.util.List;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class SpringSecurityConfiguration {

    private final Environment environment;
    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        /*
        Skru av Spring Security for alt under internal.
        Begrunnelse for å bruke dette i stede for å ta http.mvcMatchers("/internal/**").permitAll()
        er at dersom credentials har utgått, så vil de trigge en reauth selv på endepunkter som er ignorert.
         */
        return (web) -> web.ignoring().mvcMatchers("/internal/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().csrf()
                    .disable()
                .authorizeRequests()
                    .anyRequest().authenticated()
                .and().exceptionHandling()
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
//                .and().oauth2ResourceServer()
//                    .jwt().decoder(jwtDecoder());
        return http.build();
    }

    private JwtDecoder jwtDecoder() {
        val audience = new HashSet<>(List.of(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.accepted-audience", String[].class)));
        return JwtUtils.jwtDecoder(oAuth2ResourceServerProperties, audience);
    }
}
