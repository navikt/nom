package no.nav.nom.apps.ui;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.NoHttpResponseException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.SocketException;
import java.util.Set;

@SpringBootApplication
@Import(UiStarter.class)
public class TestConfig {
    public static final String RSA_KEY_ID = "keyId";

    @Bean
    @SneakyThrows
    RSAKey rsaKey() {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(RSA_KEY_ID)
                .generate();
    }

    /**
     * Pga Connection-reuse så ender det opp med å måtte gå flere runder,
     * men ønsker ikke å kjøre med Retry enabled fordi 4xx og 5xx vil da også bli forsøkt på nytt.
     * Det er bare issuet med connection-reuse som skal feile her og prøves på nytt.
     */
    @Bean
    RestTemplateCustomizer restTemplateCustomizer() {
        val retryable = Set.of(NoHttpResponseException.class, SocketException.class);

        return restTemplate -> restTemplate.getInterceptors().add((request, body, execution) -> {
            while (true) {
                try {
                    return execution.execute(request, body);
                } catch (Exception e) {
                    if (!retryable.contains(e.getClass())) {
                        throw e;
                    }
                }
            }
        });
    }
}
