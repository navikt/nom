package no.nav.nom.apps.api.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class OAuth2ResourceServerConfiguration extends WebSecurityConfigurerAdapter {

    private final JWTFilter jwtFilter;

    @Override
    public void configure(WebSecurity web) {
        /*
        Skru av Spring Security for alt under internal.
        Begrunnelse for å bruke dette i stede for å ta http.mvcMatchers("/internal/**").permitAll()
        er at dersom credentials har utgått, så vil de trigge en reauth selv på endepunkter som er ignorert.
         */
        web.ignoring().mvcMatchers("/internal/**", "/playground/**", "/voyager/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable()
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .addFilterBefore(jwtFilter, RequestHeaderAuthenticationFilter.class);
    }
}
