package no.nav.nom.skjermede.personer.foresporsel;

import no.nav.nom.skjermede.personer.foresporsel.api.v1.ForesporselController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.ws.rs.HttpMethod;

@Profile("!test")
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers(
                        HttpMethod.GET,
                        String.format("%s/%s", ForesporselController.BASE_URL, ForesporselController.FORESPORSEL_BEKREFTELSE))
                .fullyAuthenticated()
                .and()
                .oauth2Client()
                .and()
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/person/personopplysninger/skjerming-proxy/oauth2/authorization/aad")
                        .authorizationEndpoint(authorization -> authorization.baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(redirection -> redirection.baseUri("/person/personopplysninger/skjerming-proxy/login/oauth2/code/aad"))
                        .successHandler(new BekreftForesporselSuccessHandler())
                );
    }
}
