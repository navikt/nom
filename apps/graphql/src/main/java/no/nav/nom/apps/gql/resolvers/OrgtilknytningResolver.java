package no.nav.nom.apps.gql.resolvers;

import no.nav.nom.apps.gql.MappingUtils;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrgtilknytningResolver {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;


    public OrgtilknytningResolver(OrgtilknytningStateRepository orgtilknytningStateRepository, RessursStateRepository ressursStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursStateRepository = ressursStateRepository;
    }


    public List<OrgTilknytning> orgtilknytninger(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgtilknytningStateRepository.getAll().stream()
            .filter(ot -> ot.orgEnhetId().equals(orgEnhet.getId()))
            .filter(o -> Util.filterByPeriodiskUtvalg(o, periodiskUtvalg))
            .map(this::tilOrgTilknytning)
            .toList();
    }

    private OrgTilknytning tilOrgTilknytning(OrgtilknytningState orgtilknytning) {
        return OrgTilknytning.builder()
            .ressurs(MappingUtils.toRessurs(ressursStateRepository.findByNavident(orgtilknytning.navident()).get()))
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .sektor(orgtilknytning.sektor())
            .tilknytningType(orgtilknytning.orgtilknytningType())
            .build();
    }
}
