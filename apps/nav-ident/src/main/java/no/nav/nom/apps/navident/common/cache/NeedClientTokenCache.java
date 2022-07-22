package no.nav.nom.apps.navident.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableCaching
public class NeedClientTokenCache {
    private final RedisCacheManager cacheManager;

    @Value("${app.redis.clientTokenCacheName}") private String cacheName;

    public String getToken(String needUUID) {
        String token =  cacheManager.getCache(cacheName).get(needUUID, String.class);
        if (token == null)
            log.debug("No token found for need %s in cache".formatted(needUUID));
        else
            log.debug("Token %s... found for need %s in cache".formatted(token.substring(0, Math.min(9, token.length())), needUUID));
        return token;
    }

    public void putToken(String needUUID, String token) {
        log.debug("Caching token %s... found for need uuid %s".formatted(token.substring(0, Math.min(9, token.length())), needUUID));
        cacheManager.getCache(cacheName).put(needUUID, token);
    }

    public void putOauth2TokenFromSession(String uuid) {
        try {
            final var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                final var token = auth.getToken().getTokenValue();
                putToken(uuid, token);
            } else {
                log.warn("SecurityContext has no Authentication at all. Cannot cache token for need %s".formatted(uuid));
            }
        } catch (ClassCastException e) {
            log.error("SecurityContext Authentication is not of expected JwtAuthenticationToken type. Cannot cache token for need %s".formatted(uuid), e);
        }
    }
}
