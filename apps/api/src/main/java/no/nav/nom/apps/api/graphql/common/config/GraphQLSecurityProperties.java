package no.nav.nom.apps.api.graphql.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

@ConstructorBinding
@ConfigurationProperties("app.graphql.auth")
public record GraphQLSecurityProperties(
        @DefaultValue Map<String, ScopedAccess> clients,
        @DefaultValue Map<String, GroupAccess> groups
) {
    /**
     * Limits the access of a given client to the default scope and other additional scopes
     * One of clientId or clientAlias is expected
     *
     * @param clientId    the azp uuid for the client
     * @param clientAlias the azp_name for the client
     * @param scopes      the set of allowed scopes for this client
     */
    public record ScopedAccess(
            String clientId,
            String clientAlias,
            Set<GraphQLAccessScope> scopes
    ) implements MappableAccessScopes {
        public ScopedAccess(
                @DefaultValue("") String clientId,
                @DefaultValue("") String clientAlias,
                Set<GraphQLAccessScope> scopes
        ) {
            if (!StringUtils.hasText(clientId) && !StringUtils.hasText(clientAlias)) {
                throw new IllegalArgumentException("One of client-id or client-alias must be set!");
            }
            this.clientId = clientId;
            this.clientAlias = clientAlias;
            this.scopes = scopes;
        }

        @Override
        public Set<String> identifiers() {
            final var set = new HashSet<String>();
            if(StringUtils.hasText(clientId)) {
                set.add(clientId);
            }
            if (StringUtils.hasText(clientAlias)) {
                set.add(clientAlias);
            }
            if(set.isEmpty()) {
                throw new IllegalStateException("client-id and client-alias is missing, at least one must be present!");
            }
            return set;
        }
    }

    public record GroupAccess(
            String groupId,
            @DefaultValue Set<GraphQLAccessScope> scopes
    ) implements MappableAccessScopes {
        @Override
        public Set<String> identifiers() {
            return singleton(groupId);
        }
    }

    public interface MappableAccessScopes {
        // identifier vil være AAD group-id, eller AD application client-id/client-name
        Set<String> identifiers();

        Set<GraphQLAccessScope> scopes();

        default Set<GrantedAuthority> authorities() {
            return scopes().stream().map(GraphQLAccessScope::asGrantedAuthority).collect(toSet());
        }
    }
}
