package no.nav.nom.apps.api;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@Import(ApiStarter.class)
class TestConfig {

    public static final String RSA_KEY_ID = "keyId";

    @Bean
    @SneakyThrows
    public RSAKey rsaKey() {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(RSA_KEY_ID)
                .generate();
    }
}
