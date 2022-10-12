package no.nav.nom.apps.api.graphql.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.Map;
import java.util.Set;

@ConstructorBinding
@ConfigurationProperties("app.graphql.preauth")
public record GraphQLSecurityProperties(
        Map<String, ScopedAccess> query,
        Map<String, ScopedAccess> mutation
) {
    public record ScopedAccess(
            Set<String> accessingClient,
            GraphQLAccessScope scope
    ) {}
}
