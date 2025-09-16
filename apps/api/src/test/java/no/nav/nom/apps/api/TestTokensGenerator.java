package no.nav.nom.apps.api;

import com.nimbusds.jose.JOSEObjectType;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.nom.apps.api.common.security.NomApiAccessScope.ADMIN;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.MUTATE_RESSURS;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_UI_APP;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_ROBOT;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_VIKAFOSSEN;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.READONLY;

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

    public String nomWriteRobotGroupAndNomUiAzp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomWriteRobotGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    public String nomWriteVikafossenGroupAndNomUiAzp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomWriteVikafossenGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    public String nomWriteUtlKonsulentGroupAndNomUiAzp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomWriteUtlKonsulentGroup"));
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

    public String dollyClient() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "dollyClient");
        return validToken(claims);
    }

    public String nomUiClient(String navident) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", navident);
        claims.put("groups", List.of("nomWriteGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    public void setNomAdminUserAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(ADMIN, NOM_UI_APP), "nomUiClient");
    }

    public void setNomWriteGroupAndNomUiAzpAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(MUTATE_RESSURS, NOM_UI_APP), "nomUiClient");
    }

    public void setNomReadOnlyGroupAndNomUiAzpAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(READONLY, NOM_UI_APP), "nomUiClient");
    }

    public void setNomWriteRobotGroupAndNomUiAzpAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(NOM_WRITE_ROBOT, NOM_UI_APP), "nomUiClient");
    }

    public void setNomWriteVikafossenGroupAndNomUiAzpAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(NOM_WRITE_VIKAFOSSEN, NOM_UI_APP), "nomUiClient");
    }

    public void setNomRemedyIntegrasjonAzpAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(NOM_REMEDY_INTEGRASJON_APP), "nomRemedyIntegrasjonClient");
    }

    public void setNoScopesUserAuth() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(), "some-app");
    }

    public void setDefaultUserNomUiClient() {
        setAuthentication(LOGGED_IN_USER_NAVIDENT, List.of(NOM_UI_APP), "nomUiClient");
    }

    public void setAuthentication(String navidentBruker, List<NomApiAccessScope> accessScopes, String azpName) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("NAVident", navidentBruker)
            .claim("azp_name", azpName)
            .build();

        Authentication auth = new JwtAuthenticationToken(jwt, accessScopes.stream().map(NomApiAccessScope::asGrantedAuthority).toList());

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

}
