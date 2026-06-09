package no.nav.nom.apps.sfintegrasjon.sfclient.oauth;

import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Thread-safe cache for a SAP SuccessFactors OAuth access token.
 * Proactively refreshes the token when fewer than 10 minutes remain before expiry.
 */
@Component
public class SfTokenCache {

    private static final Logger log = LoggerFactory.getLogger(SfTokenCache.class);
    private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(10);

    private final SfOAuthService sfOAuthService;

    private volatile SfAccessToken cachedToken;
    private volatile Instant expiresAt;

    public SfTokenCache(SfOAuthService sfOAuthService) {
        this.sfOAuthService = sfOAuthService;
    }

    /**
     * Returns the cached access token, refreshing it first if it is absent or
     * expires within {@value #REFRESH_THRESHOLD} minutes.
     */
    public synchronized SfAccessToken getToken() {
        if (cachedToken == null || isNearExpiry()) {
            log.info("SF access token absent or near expiry — refreshing via SAML bearer grant");
            refresh();
        }
        return cachedToken;
    }

    /**
     * Invalidates the cached token and forces an immediate refresh on the next {@link #getToken()} call.
     */
    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = null;
    }

    private boolean isNearExpiry() {
        return expiresAt == null || Instant.now().isAfter(expiresAt.minus(REFRESH_THRESHOLD));
    }

    private void refresh() {
        cachedToken = sfOAuthService.generateToken(SamlAssertionParams.defaults());
        expiresAt = Instant.now().plusSeconds(cachedToken.expiresIn());
        log.info("SF access token refreshed, expires at {}", expiresAt);
    }
}
