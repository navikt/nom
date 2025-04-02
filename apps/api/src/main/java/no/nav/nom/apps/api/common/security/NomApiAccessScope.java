package no.nav.nom.apps.api.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum NomApiAccessScope {
    QUERY_PERSON("QUERY_PERSON"),
    MUTATE_RESSURS("MUTATE_RESSURS"),
    ADMIN("ADMIN"),
    TEAMCAT("TEAMCAT"),
    NOM_UI_APP("NOM_UI_APP"),
    NOM_PILOT("NOM_PILOT"),
    NOM_REMEDY_INTEGRASJON_APP("NOM_REMEDY_INTEGRASJON_APP"),
    LOCAL_NOM_API("LOCAL_NOM_API"),
    NOM_WRITE_UTL_KONSULENT("NOM_WRITE_UTL_KONSULENT"),
    NOM_WRITE_VIKAFOSSEN("NOM_WRITE_VIKAFOSSEN"),
    NOM_WRITE_ROBOT("NOM_WRITE_ROBOT");

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
