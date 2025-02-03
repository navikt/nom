package no.nav.nom.apps.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Component
public class AZPAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final static Logger log = LoggerFactory.getLogger(AZPAuthoritiesConverter.class);
    private final static String GROUPS_CLAIM_NAME = "groups";
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
    private final AppGrantedAuthoritiesProperties securityProperties;
    private final Map<String, Set<GrantedAuthority>> authoritiesMap = new HashMap<>();

    public AZPAuthoritiesConverter(AppGrantedAuthoritiesProperties securityProperties) {
        this.securityProperties = securityProperties;

        securityProperties.groups().forEach(this::calculateScopes);
    }

    private void calculateScopes(String configuration, AppGrantedAuthoritiesProperties.MappableAccessScopes mappableAccessScopes) {
        final var authorities = mappableAccessScopes.authorities();

        mappableAccessScopes.identifiers().forEach(identifier ->
            authoritiesMap.merge(identifier, authorities, (oldAuth, newAuth) -> {
                oldAuth.addAll(newAuth);
                log.debug("Current authorities associated with identifier \"{}\" is {}", identifier, oldAuth);
                return oldAuth;
            })
        );

        log.info(
            "Adding authorities {} from configuration \"{}\" with identifier(s) = {}",
            mappableAccessScopes.authorities(),
            configuration,
            mappableAccessScopes.identifiers()
        );
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final var grantedAuthorities = defaultConverter.convert(jwt);

        if (!securityProperties.groups().isEmpty()) {
            List<String> claimValues = Optional.ofNullable(jwt.getClaimAsStringList(GROUPS_CLAIM_NAME)).orElse(emptyList());
            claimValues.forEach(value -> findAndApplyAuthorities(grantedAuthorities, GROUPS_CLAIM_NAME, value));
        }

        return grantedAuthorities;
    }

    private void findAndApplyAuthorities(Collection<GrantedAuthority> grantedAuthorities, String claimPropertyName, String accessor) {
        final var authorities = authoritiesMap.getOrDefault(accessor, emptySet());

        if (log.isTraceEnabled()) {
            log.trace("Mapping claims from \"{}\" = \"{}\" with resolved authorities: {}", claimPropertyName, accessor, authorities);
        }

        grantedAuthorities.addAll(authorities);
    }
}
