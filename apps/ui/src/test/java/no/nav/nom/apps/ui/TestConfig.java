package no.nav.nom.apps.ui;

import lombok.val;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.apache.http.NoHttpResponseException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.SocketException;
import java.util.Set;

@SpringBootApplication
@Import({SecurityTestUtils.class, UiStarter.class})
public class TestConfig {

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
