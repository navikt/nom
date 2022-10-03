package no.nav.nom.apps.navident.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.*;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableCaching
public class RnRMdcCache {
    private final RedisCacheManager cacheManager;
    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    private static String CACHE_NAME="mdc-cache";

    public void loadMdc(String needUUID) {
        MDC.put(MDC_CONSUMER_ID_KEY,    cacheManager.getCache(CACHE_NAME + MDC_CONSUMER_ID_KEY  ).get(needUUID, String.class));

        String callId = cacheManager.getCache(CACHE_NAME + MDC_CALL_ID_KEY).get(needUUID, String.class);
        MDC.put(MDC_CALL_ID_KEY, callId);

        String xRequestId = cacheManager.getCache(CACHE_NAME + MDC_X_REQUEST_ID_KEY).get(needUUID, String.class);
        if (xRequestId != null) {
            MDC.put(MDC_X_REQUEST_ID_KEY, xRequestId);
        } else {
            MDC.put(MDC_X_REQUEST_ID_KEY, callId);
        }
    }

    public void cacheMdc(String needUUID) {
        cacheManager.getCache(CACHE_NAME + MDC_CALL_ID_KEY      ).put(needUUID, MDC.get(MDC_CALL_ID_KEY));
        cacheManager.getCache(CACHE_NAME + MDC_CONSUMER_ID_KEY  ).put(needUUID, MDC.get(MDC_CONSUMER_ID_KEY));
        String xRequestId = MDC.get(MDC_X_REQUEST_ID_KEY) ;
        if (xRequestId != null) {
            cacheManager.getCache(CACHE_NAME + MDC_X_REQUEST_ID_KEY).put(needUUID, xRequestId);
        }
    }
}
