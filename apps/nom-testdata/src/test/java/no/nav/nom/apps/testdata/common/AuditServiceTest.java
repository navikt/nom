package no.nav.nom.apps.testdata.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceTest {
    private final AuditService auditService = new AuditService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsAndTrimsNavidentFromAuthenticatedJwt() {
        SecurityContextHolder.getContext().setAuthentication(authentication("  synthetic-ident  "));
        var endring = auditService.endring();
        assertThat(endring.bruker()).isEqualTo("synthetic-ident");
    }

    @Test
    void usesFallbackWithoutAuthentication() {
        SecurityContextHolder.clearContext();
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");
    }

    @Test
    void usesFallbackWithoutNavidentClaim() {
        var jwt = Jwt.withTokenValue("synthetic-token").header("alg", "RS256").subject("synthetic-user").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void usesFallbackForBlankClaim(String ident) {
        SecurityContextHolder.getContext().setAuthentication(authentication(ident));
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");
    }

    @Test
    void usesFallbackForNonStringClaim() {
        var jwt = Jwt.withTokenValue("synthetic-token").header("alg", "RS256")
            .subject("synthetic-user").claim("NAVident", List.of("synthetic-ident")).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");
    }

    @Test
    void doesNotTrustUnauthenticatedJwt() {
        var authentication = authentication("synthetic-ident");
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");
    }

    @Test
    void doesNotUseAnonymousOrNonJwtPrincipalAsAuditActor() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "test-key",
            "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "synthetic-ident", "not-a-credential", List.of()));
        assertThat(auditService.endring().bruker()).isEqualTo("nom-testdata");
    }

    private static JwtAuthenticationToken authentication(String ident) {
        var jwt = Jwt.withTokenValue("synthetic-token").header("alg", "RS256")
            .subject("synthetic-user").claim("NAVident", ident).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("NOM_ADMIN")));
    }
}
