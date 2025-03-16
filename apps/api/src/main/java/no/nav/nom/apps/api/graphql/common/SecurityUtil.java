package no.nav.nom.apps.api.graphql.common;

import graphql.schema.DataFetchingFieldSelectionSet;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class SecurityUtil {

    private final static Logger logger = LoggerFactory.getLogger(SecurityUtil.class);

    private final boolean securityEnabled;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public static final String NAVIDENT = "NAVident";
    public static final String AZP_NAME = "azp_name";

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
                if (logger.isTraceEnabled()) {
                    logger.trace("Anonymous user blocked: {}", authentication);
                }
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
                new String[]{NomApiAccessScope.QUERY_PERSON.getScope()}
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
     * @param requiredRoles     the roles required to access the fields from fieldGlobPatterns
     * @return true if the user has access, or if security is disabled
     * @throws AccessDeniedException if user does not have access to the fields specified
     */
    public boolean hasRequiredAccess(
            Authentication authentication, DataFetchingFieldSelectionSet fieldSelectionSet, String[] fieldGlobPatterns, String[] requiredRoles
    ) {
        if (securityEnabled) {
            var authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            if (logger.isTraceEnabled()) {
                logger.trace("Checking access to fields {} with required roles {} for current authorities {}", fieldGlobPatterns, requiredRoles, authorities);
            }

            if (fieldSelectionSet.containsAnyOf("", fieldGlobPatterns) && Arrays.stream(requiredRoles).noneMatch(authorities::contains)) {
                throw new AccessDeniedException(String.format("User does not have the correct access rights for fields %s", List.of(fieldGlobPatterns)));
            }

            if (logger.isTraceEnabled()) {
                logger.trace("User did not query any of {}, granting access!", List.of(fieldGlobPatterns));
            }
        }

        return true;
    }

    public Optional<String> getAzpName() {
        if (securityEnabled) {
            final var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken token) {
                var azp_name = token.getTokenAttributes().get(AZP_NAME);
                return Optional.ofNullable(azp_name).map(Objects::toString);
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getNavIdent(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Optional.ofNullable(jwt.getClaimAsString(NAVIDENT));
    }

    // When consumer is an application
    public static Optional<String> getAzpName(Authentication authentication){
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Optional.ofNullable(jwt.getClaimAsString(AZP_NAME));
    }


    public static boolean loggedInUserHasAccessScope(NomApiAccessScope nomApiAccessScope) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(nomApiAccessScope.asGrantedAuthority());
    }

    public static String getAuthenticatedNavIdent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getNavIdent(authentication).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Couldn't find %s in Claims", NAVIDENT)));
    }

    public static String getAuthenticatedAzpName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getAzpName(authentication).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Couldn't find %s in Claims", AZP_NAME)));
    }

    // to be deleted
    public static void printTestToken(String prefix) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken oauthToken = (JwtAuthenticationToken) authentication;
        logger.info(prefix + " getBehalfOfUserNavident Token.getTokenAttributes: {}", oauthToken.getTokenAttributes());
        logger.info(prefix + " getBehalfOfUserNavident Token: {}", oauthToken.getToken());
        logger.info(prefix + " getBehalfOfUserNavident Token.getClaimAsStringList: {}", oauthToken.getToken().getClaimAsStringList("groups"));
        logger.info(prefix + " getBehalfOfUserNavident Token.getPrincipal: {}", oauthToken.getPrincipal());
        logger.info(prefix + " getBehalfOfUserNavident Token.getAuthorities: {}", oauthToken.getAuthorities());
        logger.info(prefix + " getBehalfOfUserNavident TokengetPrincipal.getClaimAsStringList: {}", ((Jwt)oauthToken.getPrincipal()).getClaimAsStringList("groups"));
    }
}
