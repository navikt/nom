package no.nav.nom.apps.api.common.security;

import no.nav.nom.apps.api.graphql.common.config.GraphQLSecurityProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public final class AZPGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String AUTHORITY_PREFIX = "SCOPE_";

    public static String GRAPHQL_QUERY_PERSON = "QUERY_PERSON";
    public static String GRAPHQL_MUTATE_RESSURS = "MUTATE_RESSURS";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
    private final GraphQLSecurityProperties securityProperties;

    public AZPGrantedAuthoritiesConverter(GraphQLSecurityProperties securityProperties) {
        defaultConverter.setAuthorityPrefix(AUTHORITY_PREFIX);
        this.securityProperties = securityProperties;
    }


    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final var grantedAuthorities = defaultConverter.convert(jwt);

        final var azp = jwt.getClaim("azp");
        if (azp instanceof String st /*&& securityProperties*.query.person.contains(azp)*/) {
            // TODO: Add correct scope from properties
            grantedAuthorities.add(new SimpleGrantedAuthority(st));
        }

        final var azp_name = jwt.getClaim("azp_name");
        if (azp_name instanceof String st /*&& securityProperties*.query.person.contains(azp)*/) {
            // TODO: Add correct scope from properties
            grantedAuthorities.add(new SimpleGrantedAuthority(st));
        }
        return grantedAuthorities;
    }

}