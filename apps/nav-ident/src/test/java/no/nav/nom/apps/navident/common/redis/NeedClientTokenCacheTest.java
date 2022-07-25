package no.nav.nom.apps.navident.common.redis;

import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.apps.navident.common.cache.NeedClientTokenCache;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import(TestRedisConfiguration.class)
@EnableCaching
class NeedClientTokenCacheTest extends SpringBootTestBase {
    @Autowired NeedClientTokenCache cache;

    @Mock AnonymousAuthenticationToken anonAuthentication;
    @Mock SecurityContext securityContext;
    @Mock Jwt jwt;

    @Test
    public void cachePutAndHit() {
        String uuid = "UUID_1";
        String token = "TOKEN_1";
        cache.putToken(uuid, token);
        String hit = cache.getToken(uuid);

        assertEquals(token, hit);
    }

    @Test
    public void putOauth2TokenFromSession() {
        String uuid = "UUID_2";
        String token = "TOKEN_2";

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(jwt.getTokenValue()).thenReturn(token);

        SecurityContextHolder.setContext(securityContext);

        cache.putOauth2TokenFromSession(uuid);
        String hit = cache.getToken(uuid);

        assertEquals(token, hit);
    }

    @Test
    public void putOauth2TokenFromSession_NoJwtToken() {
        String uuid = "UUID_3";
        String token = "TOKEN_3";

        when(securityContext.getAuthentication()).thenReturn(anonAuthentication);

        SecurityContextHolder.setContext(securityContext);

        cache.putOauth2TokenFromSession(uuid);
        String hit = cache.getToken(uuid);

        assertNull(hit);
    }

}
