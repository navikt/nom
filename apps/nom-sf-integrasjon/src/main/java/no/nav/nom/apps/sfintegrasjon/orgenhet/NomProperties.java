package no.nav.nom.apps.sfintegrasjon.orgenhet;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.nom-api")
public record NomProperties(
    String baseUrl
) {

}
