package no.nav.nom.apps.ui.azure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenRotatingFilter extends OncePerRequestFilter {
    private final OAuth2AuthorizedClientRepository clientRepository;
    private final AadConsumer aadConsumer;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain fc) throws ServletException, IOException {
        try {

            val authenticated = Optional.ofNullable(SecurityContextHolder.getContext())
                    .map(e -> e.getAuthentication())
                    .map(e -> e.isAuthenticated())
                    .orElse(false);

            if (authenticated) {
                rotateAccessKeyIfExpired(req, res);
            }
        } catch (RuntimeException e) {
            log.error("Failed to rotate access token", e);
        }

        fc.doFilter(req, res);
    }

    private void rotateAccessKeyIfExpired(HttpServletRequest req, HttpServletResponse res) {
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
