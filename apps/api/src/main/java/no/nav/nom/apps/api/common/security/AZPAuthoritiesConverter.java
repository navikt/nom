package no.nav.nom.apps.api.common.security;

import no.nav.nom.apps.api.graphql.common.config.GraphQLAccessScope;
import no.nav.nom.apps.api.graphql.common.config.GraphQLSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;

@Component
public final class AZPAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final static Logger log = LoggerFactory.getLogger(AZPAuthoritiesConverter.class);
    private static final String AUTHORITY_PREFIX = "SCOPE_";

    private final static Set<String> AZP_CLAIM_NAMES = Set.of("azp", "azp_name");

    public static final String AUTH_QUERY_PERSON = "QUERY_PERSON";
    public static final String AUTH_MUTATE_RESSURS = "MUTATE_RESSURS";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
    private final GraphQLSecurityProperties securityProperties;
    private final Map<String, Set<GraphQLAccessScope>> accessMap = new HashMap<>();

    public AZPAuthoritiesConverter(GraphQLSecurityProperties securityProperties) {
        defaultConverter.setAuthorityPrefix(AUTHORITY_PREFIX);
        this.securityProperties = securityProperties;

        preComputeAccessFor(securityProperties.clients());
    }

    private void preComputeAccessFor(Map<String, GraphQLSecurityProperties.ScopedAccess> accessors) {
        accessors.forEach((client, scopedAccess) -> {
            if(StringUtils.hasText(scopedAccess.clientId())) {
                calculateScopes(client, scopedAccess.clientId(), false, scopedAccess.scopes());
            }

            if(StringUtils.hasText(scopedAccess.clientAlias())) {
                calculateScopes(client, scopedAccess.clientAlias(), true, scopedAccess.scopes());
            }
        });
    }

    private void calculateScopes(String client, String clientId, boolean isAlias, Set<GraphQLAccessScope> newScopes) {
        accessMap.compute(clientId, (key, scopes) -> {
            if (scopes == null) {
                log.debug("Adding authorities {} from client-configuration \"{}\" with client{} \"{}\"", newScopes, client, isAlias ? "Alias": "Id", clientId);
                return new HashSet<>(newScopes);
            }
            scopes.addAll(newScopes);
            log.trace("Current authorities associated with client{} \"{}\" is {}", isAlias ? "Alias": "Id", clientId, scopes);
            return scopes;
        });
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final var grantedAuthorities = defaultConverter.convert(jwt);

        AZP_CLAIM_NAMES.forEach( claimName -> {
            String claimProperty = jwt.getClaim(claimName);
            var scopes = accessMap.getOrDefault(claimProperty, emptySet());

            if(log.isTraceEnabled()) {
                log.trace("Mapping claims from \"{}\" = \"{}\" with resolved authorities: {}", claimName, claimProperty, scopes);
            }

            addGrantedAuthorities(grantedAuthorities, scopes);
        });

        return grantedAuthorities;
    }

    private void addGrantedAuthorities(Collection<GrantedAuthority> grantedAuthorities, Set<GraphQLAccessScope> scopes) {
        scopes.forEach(scope -> grantedAuthorities.add(new SimpleGrantedAuthority(scope.getScope())));
    }
}