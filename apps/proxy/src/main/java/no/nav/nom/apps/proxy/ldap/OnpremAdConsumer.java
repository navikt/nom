package no.nav.nom.apps.proxy.ldap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.ad.AdRessurs;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnpremAdConsumer {

    private static final String MAIL_ATTR = "mail";
    private static final String IDENT_ATTR = "sAMAccountName";
    private static final String DISPLAY_NAME_ATTR = "displayName";
    private static final String GIVEN_NAME_ATTR = "givenName";
    private static final String SURNAME_ATTR = "sn";

    private final LdapTemplate ldapTemplate;

    public List<AdRessurs> findRessurser(List<String> navIdenter) {
        if (navIdenter.isEmpty()) {
            return List.of();
        }

        LdapQuery query = query()
                .searchScope(SearchScope.SUBTREE)
                .timeLimit(3000)
                .attributes(MAIL_ATTR, IDENT_ATTR, DISPLAY_NAME_ATTR, GIVEN_NAME_ATTR, SURNAME_ATTR)
                .where("objectClass").is("user")
                .and(buildQuery(query(), navIdenter));
        var result = new ArrayList<>(ldapTemplate.search(query, new Mapper()));

        var missingIdenter = new ArrayList<>(navIdenter);
        result.forEach(i -> missingIdenter.remove(i.getNavIdent()));
        missingIdenter.forEach(i -> result.add(AdRessurs.ikkeFunnet(i)));
        return result;
    }

    private ContainerCriteria buildQuery(LdapQueryBuilder query, List<String> navIdenter) {
        return navIdenter.stream().reduce(
                query.where(IDENT_ATTR).is(navIdenter.get(0)),
                (cc, s) -> cc.or(IDENT_ATTR).is(s), ContainerCriteria::or);
    }

    private static class Mapper extends AbstractContextMapper<AdRessurs> {

        @Override
        protected AdRessurs doMapFromContext(DirContextOperations ctx) {
            return AdRessurs.builder()
                    .email(ctx.getStringAttribute(MAIL_ATTR))
                    .navIdent(ctx.getStringAttribute(IDENT_ATTR))
                    .displayName(ctx.getStringAttribute(DISPLAY_NAME_ATTR))
                    .givenName(ctx.getStringAttribute(GIVEN_NAME_ATTR))
                    .surname(ctx.getStringAttribute(SURNAME_ATTR))
                    .build();
        }
    }

}
