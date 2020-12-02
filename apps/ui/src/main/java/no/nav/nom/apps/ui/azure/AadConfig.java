package no.nav.nom.apps.ui.azure;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
class AadConfig {

    @Bean
    @SneakyThrows
    ConfidentialClientApplication msalConfidentialClient(ClientRegistrationRepository repository) {
        val client = repository.findByRegistrationId("aad");
        Assert.notNull(client, "Failed to find registration with key aad");
        val authUrl = client.getProviderDetails().getAuthorizationUri();
        return ConfidentialClientApplication.builder(client.getClientId(), ClientCredentialFactory.createFromSecret(client.getClientSecret()))
                .authority(authUrl)
                .build();
    }
}
