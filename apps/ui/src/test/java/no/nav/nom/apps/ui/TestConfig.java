package no.nav.nom.apps.ui;

import com.microsoft.aad.msal4j.IConfidentialClientApplication;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.SneakyThrows;
import no.nav.nom.apps.ui.azure.AadOAuth2AccessTokenResponseClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@SpringBootApplication
@Import(UiStarter.class)
public class TestConfig {
    public static final String RSA_KEY_ID = "keyId";

    @Bean
    @SneakyThrows
    AadOAuth2AccessTokenResponseClient tokenResponseClient() {
        // Hadde håpet på å bruke Wiremock til å også få med MS sitt rammeverk (msal/ConfidentialClientApplication)
        // men ettersom de har så mye validering, private static final fields og package private klasser som må modifiseres,
        // ser jeg ingen annen vei enn å mocke ut hele denne klassen. Hvis det kommer noen ny versjon av msal som "gjør litt mindre", så gjerne bytt ut.
        return mock(AadOAuth2AccessTokenResponseClient.class);
    }

    @Bean
    IConfidentialClientApplication confidentialClientApplication() {
        return mock(IConfidentialClientApplication.class);
    }

    @Bean
    @SneakyThrows
    RSAKey rsaKey() {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(RSA_KEY_ID)
                .generate();
    }
}
