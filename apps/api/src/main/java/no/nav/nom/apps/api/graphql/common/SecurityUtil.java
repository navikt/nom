package no.nav.nom.apps.api.graphql.common;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import no.nav.nom.apps.api.common.security.AZPGrantedAuthoritiesConverter;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
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
     * @return true if authentication is accepted
     * @throws InvalidBearerTokenException when the token is missing or invalid
     */
    public boolean checkAuth(Authentication authentication) {
        if (securityEnabled) {
            if (trustResolver.isAnonymous(authentication)) {
                throw new InvalidBearerTokenException("Missing or invalid token");
            }
        }

        return true;
    }

    /**
     * Field Selection auth check for any query trying to access a field with a protected property.
     * Intended use is at query level with @PreAuthorize(@securityUtil.hasPersonIdentAccess(authentication, #dataFetchingEnvironment.selectionSet))
     *
     * @param authentication    the user we're checking if it has access
     * @param fieldSelectionSet the DataFetchingFieldSelectionSet we're checking
     * @return true if the user has access
     */
    public boolean hasPersonIdentAccess(Authentication authentication, DataFetchingFieldSelectionSet fieldSelectionSet) {
        return hasRequiredAccess(
                authentication,
                fieldSelectionSet,
                new String[]{"**/personIdent", "**/personident"},
                new String[]{AZPGrantedAuthoritiesConverter.GRAPHQL_QUERY_PERSON}
        );
    }

    /**
     * Field Selection auth check for any query trying to access a field with a protected property
     * Primarly intended for creating shorthand access controls like {@link SecurityUtil#hasPersonIdentAccess(Authentication, DataFetchingFieldSelectionSet)}
     * Can also be used in a controller at query level with @PreAuthorize(@securityUtil.hasPersonIdentAccess(authentication, #dataFetchingEnvironment.selectionSet, {'**\fieldA', '**\fieldB'}, 'ROLE_B'))
     *
     * @param authentication    the user we're checking if it has access
     * @param fieldSelectionSet the DataFetchingFieldSelectionSet we're checking
     * @param fieldGlobPatterns the fields we'd like to check if a user has access to
     * @param requiredRole      the roles required to access the fields from fieldGlobPatterns
     * @return true if the user has access, or if security is disabled
     * @throws AccessDeniedException if user does not have access to the fields specified
     */
    public boolean hasRequiredAccess(Authentication authentication, DataFetchingFieldSelectionSet fieldSelectionSet, String[] fieldGlobPatterns, String[] requiredRole) {
        if (securityEnabled) {
            var authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            if (fieldSelectionSet.containsAnyOf("", fieldGlobPatterns)
                    && Arrays.stream(requiredRole).noneMatch(authorities::contains)) {
                throw new AccessDeniedException(String.format("User does not have the correct access rights for fields %s", List.of(fieldGlobPatterns)));
            }
        }
        return true;
    }

    public Optional<String> getAzpName() {
        if (securityEnabled) {
            final var auth = SecurityContextHolder.getContext().getAuthentication();
            var azp_name = ((JwtAuthenticationToken) auth).getTokenAttributes().get("azp_name");

            return Optional.ofNullable(azp_name != null ? azp_name.toString() : null);
        }
        return Optional.empty();
    }

    public Optional<String> getAzp() {
        if (securityEnabled) {
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
