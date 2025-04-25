package no.nav.nom.apps.api;

import com.nimbusds.jose.JOSEObjectType;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TestTokensGenerator {

    @Autowired
    protected MockOAuth2Server mockOAuth2Server;

    public static String LOGGED_IN_USER_NAVIDENT = "A321321";
    public static String OAUTH_VALID_AUDIENCE = "test";
    public static String OAUTH_VALID_ISSUER = "aad";

    public String generateToken(String issuer, Map<String, Object> claims, String audience) {
        return mockOAuth2Server.issueToken(
            issuer,
            "clientId",
            new DefaultOAuth2TokenCallback(
                "aad",
                "subjectId",
                JOSEObjectType.JWT.getType(),
                List.of(audience),
                claims,
                3600
            )
        ).serialize();
    }

    public String validToken(Map<String, Object> claims) {
        return generateToken(OAUTH_VALID_ISSUER, claims, OAUTH_VALID_AUDIENCE);
    }



    public String nomAdminUser() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomAdminGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    public String nomWriteGroupAndNomUiAzp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomWriteGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    public String nomReadOnlyGroupAndNomUiAzp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("readOnlyGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    public String nomRemedyIntegrasjonAzp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "nomRemedyIntegrasjonClient");
        return validToken(claims);
    }

    public String noScopesUser() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("azp_name", "some-app");
        return validToken(claims);
    }

    public String queryPersonClient() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "queryPersonClient");
        return validToken(claims);
    }

    public String queryPersonClient_withDifferentSubAndOidAttributes() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "queryPersonClient");
        // different values of sub and oid attributes in the token indicates that the token is ContextToken For an UserPrincipal
        claims.put("sub", "subject");
        claims.put("oid", "object");
        return validToken(claims);
    }

    public String mutateRessursClient() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "mutateRessursClient");
        return validToken(claims);
    }

    public String noScopesClient() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "noScopesClient");
        return validToken(claims);
    }

    public String queryReprFullmaktClient() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "dev-fss:repr:repr-fullmakt");
        return validToken(claims);
    }

    public String queryOmadaClient() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "queryOmadaClient");
        return validToken(claims);
    }

    public String nomUiClient(String navident) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", navident);
        claims.put("groups", List.of("nomWriteGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }



}
