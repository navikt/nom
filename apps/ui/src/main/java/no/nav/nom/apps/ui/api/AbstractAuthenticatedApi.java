package no.nav.nom.apps.ui.api;

import lombok.val;
import no.nav.nom.apps.ui.azure.AadConsumer;
import no.nav.nom.apps.ui.common.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;

abstract class AbstractAuthenticatedApi {

    @Autowired
    protected AppProperties properties;

    @Autowired
    protected OAuth2AuthorizedClientRepository clientRepository;

    @Autowired
    protected AadConsumer aadConsumer;

    protected String getOnBehalfOfToken(HttpServletRequest req, Set<String> scopes) {
        val currentAuth = SecurityContextHolder.getContext().getAuthentication();
        val currentClient = clientRepository.loadAuthorizedClient("aad", currentAuth, req);

        // Bare en string som alltids matcher samme scopes, kan også f.eks bruke hashing.
        val oboSessionKey = URLEncoder.encode(scopes.stream().sorted().collect(joining(",")), Charset.defaultCharset());
        val existingToken = Optional.ofNullable(req.getSession(false))
                .map(e -> e.getAttribute(oboSessionKey))
                .map(e -> (OAuth2AccessToken) e);

        if (doFetchOnBehalfOf(existingToken)) {
            val result = aadConsumer.getOnBehalfOfTokenFor(currentClient.getAccessToken().getTokenValue(), scopes);
            req.getSession().setAttribute(oboSessionKey, result.getAccessToken());
            return result.getAccessToken().getTokenValue();
        } else {
            return existingToken.get().getTokenValue();
        }
    }

    private boolean doFetchOnBehalfOf(Optional<OAuth2AccessToken> current) {
        return current
                // 10 Sec left of On-Behalf-Of token, refresh. Note, this only works if there is a refresh on the access token!
                // This is because the OBO will only last as long as the users access-token.
                .filter(e -> Duration.between(Instant.now(), e.getExpiresAt()).getSeconds() > 10)
                .isEmpty();
    }
}
