package no.nav.nom.apps.navident.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Component
public class GrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String groupsClaimName = "groups";

    @Value("${GROUP_ID_ADMIN}")
    String adminADGroupId;
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        final var grantedAuthorities = defaultConverter.convert(jwt);
        final var jwtGroups = Optional.ofNullable(jwt.getClaimAsStringList(groupsClaimName)).orElse(new ArrayList<>());
        if (jwtGroups.contains(adminADGroupId)) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            log.info("adminADGroupId '{}' added med 'ROLE_ADMIN' ", adminADGroupId);
        }
        return grantedAuthorities;
    }
}
