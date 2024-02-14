package no.nav.nom.apps.api.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum NomApiAccessScope {
    QUERY_PERSON("QUERY_PERSON"),
    MUTATE_RESSURS("MUTATE_RESSURS"),
    ADMIN("ADMIN"),
    TEAMCAT("TEAMCAT"),
    NOM_UI_APP("NOM_UI_APP");

    private final String scope;

    NomApiAccessScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public GrantedAuthority asGrantedAuthority() {
        return new SimpleGrantedAuthority(scope);
    }
}
