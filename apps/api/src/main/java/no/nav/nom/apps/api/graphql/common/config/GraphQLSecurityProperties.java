package no.nav.nom.apps.api.graphql.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

@ConstructorBinding
@ConfigurationProperties("app.graphql.auth")
public record GraphQLSecurityProperties(
        @DefaultValue Map<String, ScopedAccess> clients
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
    ) {
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
    }
}
