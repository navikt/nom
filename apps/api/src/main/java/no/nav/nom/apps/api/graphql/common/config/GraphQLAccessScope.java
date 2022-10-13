package no.nav.nom.apps.api.graphql.common.config;

import no.nav.nom.apps.api.common.security.AZPGrantedAuthoritiesConverter;

public enum GraphQLAccessScope {
    QUERY_PERSON(AZPGrantedAuthoritiesConverter.GRAPHQL_QUERY_PERSON),
    MUTATE_RESSURS(AZPGrantedAuthoritiesConverter.GRAPHQL_MUTATE_RESSURS);

    private final String scope;
    GraphQLAccessScope(String scope) {
        this.scope = scope;
    }

    public String getScopeWithPrefix(String scopePrefix) {
        return scopePrefix + scope;
    }

    public String getScope() {
        return scope;
    }
}
