package no.nav.nom.apps.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

@ConfigurationProperties("app.granted-authorities")
public record AppGrantedAuthoritiesProperties(
    @DefaultValue Map<String, GroupAccess> groups
) {
    public record GroupAccess(
        String groupId,
        @DefaultValue Set<NomApiAccessScope> scopes
    ) implements MappableAccessScopes {
        @Override
        public Set<String> identifiers() {
            return singleton(groupId);
        }
    }

    public interface MappableAccessScopes {
        // identifier vil være AAD group-id, eller AD application client-id/client-name
        Set<String> identifiers();

        Set<NomApiAccessScope> scopes();

        default Set<GrantedAuthority> authorities() {
            return scopes().stream().map(NomApiAccessScope::asGrantedAuthority).collect(toSet());
        }
    }
}
