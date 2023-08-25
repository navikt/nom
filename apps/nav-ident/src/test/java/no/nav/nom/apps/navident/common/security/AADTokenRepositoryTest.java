package no.nav.nom.apps.navident.common.security;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({SecurityTestUtils.class})
class AADTokenRepositoryTest extends SpringBootTestBase {

    @Autowired
    private AADTokenRepository aadTokenRepository;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Test
    public void getOnbehalfofTokenFor_illegalAssertionProvided() {
        String obo = aadTokenRepository.getOnbehalfofTokenFor(null, "test");
        assertNull(obo);

        assertThrows(ParseException.class, () -> aadTokenRepository.getOnbehalfofTokenFor("", "test"));

        assertThrows(ParseException.class, () -> aadTokenRepository.getOnbehalfofTokenFor("rubbish", "test"));
    }

    @Test
    public void getOnbehalfofTokenFor_ValidAssertionProvided() {
        String assertion = getValidToken();
        String returningObo = getValidToken();

        stubFor(post("/oauth2/v2.0/token")
                .withHeader("Content-Type", containing("x-www-form-urlencoded"))
                .withRequestBody(containing("assertion=%s".formatted(assertion)))
                .willReturn(okJson("{ " +
                        "      \"token_type\": \"Bearer\",\n" +
                        "      \"expires_in\": 3599,\n" +
                        "      \"access_token\": \"" + returningObo + "\"" +
                        "}")
                )
        );

        String obo = aadTokenRepository.getOnbehalfofTokenFor(assertion, "test");
        assertEquals(returningObo, obo);

        obo = aadTokenRepository.getOnbehalfofTokenFor(assertion, "test");
        assertEquals(returningObo, obo);

        verify(1, postRequestedFor(urlEqualTo("/oauth2/v2.0/token"))); // Make sure cache is hit on second invocation
    }

    @SneakyThrows
    private String getValidToken() {
        val jwtClaimsBuilder = securityTestUtils.validJwtClaims();
        return securityTestUtils.createToken(jwtClaimsBuilder);
    }
}