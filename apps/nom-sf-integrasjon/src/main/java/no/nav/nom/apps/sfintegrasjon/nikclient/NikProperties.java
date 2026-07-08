package no.nav.nom.apps.sfintegrasjon.nikclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.nik")
public record NikProperties(
    String baseUrl,
    String aadScope
) {
}
