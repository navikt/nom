package no.nav.nom.apps.api.graphql.common.config;

import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum GraphQLAccessScope {
    QUERY_PERSON(AZPAuthoritiesConverter.AUTH_QUERY_PERSON),
    MUTATE_RESSURS(AZPAuthoritiesConverter.AUTH_MUTATE_RESSURS);

    private final String scope;

    GraphQLAccessScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public GrantedAuthority asGrantedAuthority() {
        return new SimpleGrantedAuthority(scope);
    }
}
