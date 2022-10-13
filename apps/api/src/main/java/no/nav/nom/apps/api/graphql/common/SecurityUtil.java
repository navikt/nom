package no.nav.nom.apps.api.graphql.common;

import graphql.schema.DataFetchingEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class SecurityUtil {

    private final boolean securityEnabled;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public SecurityUtil(Environment environment) {
        securityEnabled = !Arrays.stream(environment.getActiveProfiles()).toList().contains("local");
    }

    /**
     * Used to check auth with spring security @Preauthorize(@securityUtil.checkAuth(authentication))
     *
     * @param authentication the authentication object to check
     * @return boolean true or false if authentication is accepted
     */
    public boolean checkAuth(Authentication authentication) {
        if (!securityEnabled) {
            return true;
        }

        if (trustResolver.isAnonymous(authentication)) {
            throw new InvalidBearerTokenException("Missing or invalid token");
        }

        return true;
    }

    public Optional<String> getAzpName(){
        if(securityEnabled) {
            final var auth = SecurityContextHolder.getContext().getAuthentication();
            var azp_name = ((JwtAuthenticationToken) auth).getTokenAttributes().get("azp_name");

            return Optional.ofNullable(azp_name != null ? azp_name.toString() : null);
        }
        return Optional.empty();
    }

    public Optional<String> getAzp(){
        if(securityEnabled) {
            final var auth = SecurityContextHolder.getContext().getAuthentication();
            var azp_name = ((JwtAuthenticationToken) auth).getTokenAttributes().get("azp");

            return Optional.ofNullable(azp_name != null ? azp_name.toString() : null);
        }
        return Optional.empty();
    }

    public Optional<String> getToken(DataFetchingEnvironment env) {
        if (securityEnabled) {
            final var auth = SecurityContextHolder.getContext().getAuthentication();
            var token = ((JwtAuthenticationToken) auth).getToken();

            return Optional.of(token.getTokenValue());
        }

        return Optional.empty();
    }
}
