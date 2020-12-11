package no.nav.nom.apps.ui.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        /*
          Default i Spring Security OAuth2 er at man lagrer innloggede brukere sine access / refresh tokens
          i en in-memory variant. Den er ikke anbefalt for produksjon og det har vært stilt spørsmål om man skal logge warnings.
          Denne overskriver default til å legge informasjonen i sessjoner i stedenfor.
         */
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }
}
