package no.nav.nom.apps.navident.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CALL_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CONSUMER_ID_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableCaching
public class RnRMdcCache {
    private final RedisCacheManager cacheManager;
    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    private static String CACHE_NAME="mdc-cache";

    public void loadMdc(String needUUID) {
        MDC.put(MDC_CALL_ID_KEY,        cacheManager.getCache(CACHE_NAME + MDC_CALL_ID_KEY      ).get(needUUID, String.class));
        MDC.put(MDC_CONSUMER_ID_KEY,    cacheManager.getCache(CACHE_NAME + MDC_CONSUMER_ID_KEY  ).get(needUUID, String.class));
    }

    public void cacheMdc(String needUUID) {
        cacheManager.getCache(CACHE_NAME + MDC_CALL_ID_KEY      ).put(needUUID, MDC.get(MDC_CALL_ID_KEY));
        cacheManager.getCache(CACHE_NAME + MDC_CONSUMER_ID_KEY  ).put(needUUID, MDC.get(MDC_CONSUMER_ID_KEY));
    }
}
