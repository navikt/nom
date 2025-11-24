package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.Util;
import no.nav.nom.apps.api.graphql.common.exception.LederNotFoundException;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.LederForRessurs;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;

@Component
public class LederResolver {

    private final OrgenhetService orgenhetService;
    private final OrgEnhetLederService orgEnhetLederService;
    private final RessursStateRepository ressursStateRepository;

    public LederResolver(OrgenhetService orgenhetService,
                         OrgEnhetLederService orgEnhetLederService,
                         RessursStateRepository ressursStateRepository
    ) {
        this.orgenhetService = orgenhetService;
        this.orgEnhetLederService = orgEnhetLederService;
        this.ressursStateRepository = ressursStateRepository;
    }

    public <T> Map<Ressurs, List<T>> load(Collection<Ressurs> ressurser, Function<Ressurs, List<T>> loader) {
        // Load a map of resources using the specified loader function
        return ressurser.stream().collect(toMap(Function.identity(), loader));
    }

    public List<LederOrgEnhet> lederFor(Ressurs ressurs) {
        return lederFor(ressurs, null);
    }

    public List<LederOrgEnhet> lederFor(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgEnhetLederService
            .getOrgEnheterNavidentErLederFor(ressurs.getNavident())
            .stream()
            .filter(orgEnhetLederState -> Util.filterByPeriodiskUtvalg(orgEnhetLederState, periodiskUtvalg))
            .map(orgEnhetLederState ->
                new LederOrgEnhet(
                    MappingUtils.toOrgEnhet(
                        orgenhetService.getById(orgEnhetLederState.orgEnhetId()).get()
                    ),
                    orgEnhetLederState.startdato(),
                    orgEnhetLederState.sluttdato()
                )
            )
            .toList();
    }

    public List<RessursLeder> ledere(Ressurs ressurs) {
        return ledere(ressurs.getNavident(), null);
    }

    public List<RessursLeder> ledere(String navIdent, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgEnhetLederService.getLedereForRessurs(navIdent, false).stream()
            .filter(lederForRessurs -> Util.filterByPeriodiskUtvalg(lederForRessurs, periodiskUtvalg))
            .map(lederForRessurs ->
                new RessursLeder(
                    MappingUtils.toRessurs(
                        ressursStateRepository.findByNavident(lederForRessurs.lederNavident()).orElseThrow(LederNotFoundException::new)
                    ),
                    lederForRessurs.erDagligOppfolging(),
                    lederForRessurs.startdato(),
                    lederForRessurs.sluttdato()
                )
            ).toList();
    }
}
