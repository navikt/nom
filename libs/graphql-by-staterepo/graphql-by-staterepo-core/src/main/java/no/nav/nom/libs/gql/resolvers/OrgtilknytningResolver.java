package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Component
public class OrgtilknytningResolver {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;

    private final OrgEnhetStateRepository orgEnhetStateRepository;


    public OrgtilknytningResolver(OrgtilknytningStateRepository orgtilknytningStateRepository, RessursStateRepository ressursStateRepository,
                                  OrgEnhetStateRepository orgEnhetStateRepository
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
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
            .primaryFysiskBrukersted(orgtilknytning.primaryFysiskBrukerstedNummer())
            .additionalFysiskeBrukerstederNummer(orgtilknytning.additionalFysiskeBrukerstederNummer())
            .build();
    }


    /*

    deprecations below

     */

    // Deprecated - 2025-09-15
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet) {
        return orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet(orgEnhet.getId())
            .stream()
            .map(this::tilOrgEnhetsKobling)
            .toList();
    }

    // Deprecated - 2025-09-15
    private OrgEnhetsKobling tilOrgEnhetsKobling(OrgtilknytningState orgtilknytning) {
        return OrgEnhetsKobling.builder()
            .ressurs(MappingUtils.toRessurs(ressursStateRepository.findByNavident(orgtilknytning.navident()).get()))
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .build();
    }

    @Deprecated
    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        return ressurser.stream()
            .collect(toMap(Function.identity(), ressurs -> this.getOrgTilknytningerForRessurs(ressurs,PeriodiskUtvalgArg.AKTIVE)));
    }



    @Deprecated
    public List<RessursOrgTilknytning> getOrgTilknytningerForRessurs(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg) {
        return orgtilknytningStateRepository
            .findByNavident(ressurs.getNavident()).stream()
            .filter(ot -> Util.filterByPeriodiskUtvalg(ot, periodiskUtvalg))
            .map(this::fromOrgtilknytningState)
            .toList();
    }

    private RessursOrgTilknytning fromOrgtilknytningState(OrgtilknytningState orgtilknytning) {
        final var orgenhet = this.orgEnhetStateRepository.findById(orgtilknytning.orgEnhetId()).get();
        final var orgEnhetGqlResponse = MappingUtils.toOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
            .orgEnhet(orgEnhetGqlResponse)
            .erDagligOppfolging(orgtilknytning.erDagligOppfolging())
            .gyldigFom(orgtilknytning.gyldigFom())
            .gyldigTom(orgtilknytning.gyldigTom())
            .sektor(orgtilknytning.sektor())
            .tilknytningType(orgtilknytning.orgtilknytningType())
            .primaryFysiskBrukersted(orgtilknytning.primaryFysiskBrukerstedNummer())
            .additionalFysiskeBrukerstederNummer(orgtilknytning.additionalFysiskeBrukerstederNummer())
            .build();
    }


}
