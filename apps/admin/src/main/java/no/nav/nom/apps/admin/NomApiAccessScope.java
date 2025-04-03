package no.nav.nom.apps.admin;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum NomApiAccessScope {
    ADMIN("ADMIN"),
    SUPPORT_READONLY("SUPPORT_READONLY"),
    READONLY("READONLY"),
    EXTENDED_READONLY("EXTENDED_READONLY");

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
