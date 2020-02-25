package no.nav.nom.skjermede.personer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.vault.annotation.VaultPropertySource;

@Profile("vault")
@Configuration(proxyBeanMethods = false)
@VaultPropertySource(value = "${vault.user.secret.path}", propertyNamePrefix = "app.", ignoreSecretNotFound = false)
public class VaultConfig {

}
