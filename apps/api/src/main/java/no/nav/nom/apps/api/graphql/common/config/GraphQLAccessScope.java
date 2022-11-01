package no.nav.nom.apps.api.graphql.common.config;

import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;

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
}
