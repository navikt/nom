package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.ressurs.api.app.RessursLedereResolver;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component
public class LederResolver {

    private final OrgenhetService orgenhetService;
    private final RessursLedereResolver ressursLedereResolver;

    public LederResolver(OrgenhetService orgenhetService, RessursLedereResolver ressursLedereResolver) {
        this.orgenhetService = orgenhetService;
        this.ressursLedereResolver = ressursLedereResolver;
    }

    public <T> Map<Ressurs, List<T>> load(Collection<Ressurs> ressurser, Function<Ressurs, List<T>> loader) {
        // Load a map of resources using the specified loader function
        return ressurser.stream().collect(toMap(Function.identity(), loader));
    }

    public List<LederOrgEnhet> lederFor(Ressurs ressurs) {
        return orgenhetService.getEnheterByLeder(ressurs.getNavIdent())
            .stream()
            .map(MappingUtils::toOrgEnhet)
            .map(LederOrgEnhet::new)
            .toList();
    }

    public List<RessursLeder> ledere(Ressurs ressurs) {
        return ledere(ressurs.getNavIdent());
    }

    public List<RessursLeder> ledere(String navIdent) {

        return ressursLedereResolver.ledere(navIdent)
            .stream()
            .map(leder -> new RessursLeder(MappingUtils.toRessurs(leder.leder()), leder.erDagligOppfolging()))
            .collect(Collectors.toList());
    }
}
