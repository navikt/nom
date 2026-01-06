package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrganiseringResolver {

    private final static Logger log = LoggerFactory.getLogger(OrganiseringResolver.class);
    private final OrganiseringStateRepository  organiseringStateRepository;

    public OrganiseringResolver(OrganiseringStateRepository organiseringStateRepository) {
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public List<Organisering> organiseringer(OrgEnhet orgEnhet, OrganiseringRetning retning, PeriodiskUtvalgArg periodiskUtvalg) {
        if (orgEnhet.endedOnStartDate()) return Collections.emptyList();

        log.debug("Henter organiseringer for {} retning '{}' og periodiskUtvalg '{}'", orgEnhet.getId(), retning, periodiskUtvalg);
        return organiseringStateRepository.getOrganiseringerContainingId(orgEnhet.getId(), false).stream()
            .filter(o -> Util.filterByPeriodiskUtvalg(o, periodiskUtvalg))
            .map(organiseringState -> convert(organiseringState, orgEnhet))
            .filter(organisering -> retning == null || organisering.getRetning() == retning)
            .toList();

    }

    private Organisering convert(OrganiseringState organisering, OrgEnhet orgEnhet) {
        var retning = OrganiseringRetning.over;

        if (organisering.idOver().equals(orgEnhet.getId())) {
            retning = OrganiseringRetning.under;
        }

        return Organisering.builder()
            .id(retning == OrganiseringRetning.over ? organisering.idOver() : organisering.id())
            .retning(retning)
            .orgEnhetNr(retning == OrganiseringRetning.over ? organisering.idOver() : organisering.id())
            .gyldigFom(organisering.gyldigFom())
            .gyldigTom(organisering.gyldigTom())
            .build();
    }
}
