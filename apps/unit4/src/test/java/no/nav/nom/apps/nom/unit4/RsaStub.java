package no.nav.nom.apps.nom.unit4;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.thomaskasene.wiremock.junit.WireMockStub;
import lombok.SneakyThrows;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class RsaStub implements WireMockStub {

    private static final String RSA_KEY_ID = "keyId";
    static final RSAKey RSA_KEY = rsaKey();

    @SneakyThrows
    private static RSAKey rsaKey() {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(RSA_KEY_ID)
                .generate();
    }

    @Override
    public void beforeAll() {
        stubKeys();
    }

    @Override
    public void beforeEach() {
        stubKeys();
    }

    private void stubKeys() {
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", RSA_KEY.toPublicJWK()))));
    }
}