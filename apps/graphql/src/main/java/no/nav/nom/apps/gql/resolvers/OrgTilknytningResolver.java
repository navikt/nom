package no.nav.nom.apps.gql.resolvers;

import no.nav.nom.apps.gql.MappingUtils;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrgTilknytningResolver {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgTilknytningResolver(OrgtilknytningStateRepository orgtilknytningStateRepository, OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }


    public List<RessursOrgTilknytning> getOrgTilknytningerForRessurs(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgtilknytningStateRepository.findByNavident(ressurs.getNavident())
            .stream()
            .filter(ot -> Util.filterByPeriodiskUtvalg(ot, periodiskUtvalg))
            .map(this::fromOrgtilknytningState)
            .toList();
    }


    private RessursOrgTilknytning fromOrgtilknytningState(OrgtilknytningState orgtilknytning) {
        final var maybeOrgenhet = orgEnhetStateRepository.findById(orgtilknytning.orgEnhetId());
        if(maybeOrgenhet.isEmpty()){
            return null;
        }

        final var orgEnhetGqlResponse = MappingUtils.toOrgEnhet(maybeOrgenhet.get());

        return RessursOrgTilknytning.builder()
            .orgEnhet(orgEnhetGqlResponse)
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .sektor(orgtilknytning.sektor())
            .tilknytningType(orgtilknytning.orgtilknytningType())
            .build();
    }
}
