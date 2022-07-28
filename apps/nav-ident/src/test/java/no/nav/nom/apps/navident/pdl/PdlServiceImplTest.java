package no.nav.nom.apps.navident.pdl;

import lombok.val;
import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.libs.test.SecurityTestUtils;
import no.nav.nom.libs.utils.common.security.spring.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({SecurityTestUtils.class})
class PdlServiceImplTest extends SpringBootTestBase {
    @Autowired
    protected PdlService pdlService;

    @Autowired protected SecurityTestUtils securityTestUtils;
    @Autowired private OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    @Test
    public void getIdenter_01019000001() {
        String ident = "01019000001";

        setSecurityContextAuthentication();
        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // kun 1 returnert, og den er gjeldende

        assertEquals(1, pldIdenter.size());
        assertEquals(ident, pldIdenter.get(0));
    }

    @Test
    public void getIdenter_01019000002() {
        String ident = "01019000002";
        String gjeldende = "01019200002";

        setSecurityContextAuthentication();
        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // 4 returnert, hvorav en aktorid, og 01019200002 er gjeldende

        assertEquals(3, pldIdenter.size());
        assertEquals(gjeldende, pldIdenter.get(0));
        assertTrue(pldIdenter.contains(ident));
    }

    @Test
    public void getIdenter_01019000003() {
        String ident = "01019000003";

        setSecurityContextAuthentication();
        List<String> pldIdenter = pdlService.getKnownPersonider(ident); // 4 returnert, hvorav en aktorid, og 01019000003 er gjeldende

        assertEquals(3, pldIdenter.size());
        assertEquals(ident, pldIdenter.get(0));
    }

    public void setSecurityContextAuthentication() {
        val builder = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(builder);
        val jwt = JwtUtils.jwtDecoder(oAuth2ResourceServerProperties).decode(token);

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}