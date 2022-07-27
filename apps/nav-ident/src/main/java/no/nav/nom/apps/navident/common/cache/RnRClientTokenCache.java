package no.nav.nom.apps.navident.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableCaching
public class RnRClientTokenCache {
    private final RedisCacheManager cacheManager;
    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    @Value("${app.redis.rnrClientTokenCacheName}") private String cacheName;

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

    public void setCachedTokedBackOnSecurityContext(String needUUID) {
        try {
            Jwt jwt = getJwtFromCacheToken(needUUID);
            if (jwt != null) SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        } catch (RuntimeException e) {
            log.error("Could not set JWT Authentication on SecurityContext", e);
        }
    }

    public Jwt getJwtFromCacheToken(String needUUID) {
        String token = getToken(needUUID);
        if (token != null) {
            return jwtDecoder(oAuth2ResourceServerProperties).decode(token);
        } else return null;
    }

    private JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        val jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(properties.getJwt().getIssuerUri());
        return jwtDecoder;
    }


}
