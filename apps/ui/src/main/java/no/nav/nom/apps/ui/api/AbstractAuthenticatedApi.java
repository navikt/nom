package no.nav.nom.apps.ui.api;

import lombok.val;
import no.nav.nom.apps.ui.azure.AadConsumer;
import no.nav.nom.apps.ui.common.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

abstract class AbstractAuthenticatedApi {

    @Autowired
    protected AppProperties properties;

    @Autowired
    protected OAuth2AuthorizedClientRepository clientRepository;

    @Autowired
    protected AadConsumer aadConsumer;

    protected void rotateAccessKeyIfExpired(HttpServletRequest req, HttpServletResponse res) {
        val currentAuth = SecurityContextHolder.getContext().getAuthentication();
        val currentClient = clientRepository.loadAuthorizedClient("aad", currentAuth, req);

        if (hasExpiredAccessToken(currentClient)) {
            val refreshToken = currentClient.getRefreshToken().getTokenValue();
            val refreshed = aadConsumer.getAccessTokenFromRefreshToken(refreshToken);

            val refreshedClient = new OAuth2AuthorizedClient(
                    currentClient.getClientRegistration(),
                    currentClient.getPrincipalName(),
                    refreshed.getAccessToken(),
                    refreshed.getRefreshToken());

            clientRepository.saveAuthorizedClient(refreshedClient, currentAuth, req, res);
        }
    }

    private boolean hasExpiredAccessToken(OAuth2AuthorizedClient client) {
        val expires = client.getAccessToken().getExpiresAt();
        val threshold = Instant.now().plusSeconds(60); // For at tokens ikke blir invalid mens de er i bruk.
        return expires.isBefore(threshold);
    }
}
