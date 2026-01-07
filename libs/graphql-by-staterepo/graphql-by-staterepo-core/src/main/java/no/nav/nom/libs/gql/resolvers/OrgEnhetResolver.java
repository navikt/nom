package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.gql.projections.OrgEnhetSearch;
import no.nav.nom.libs.gql.projections.OrgEnheterSearch;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Component
public class OrgEnhetResolver {

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final AgressoOrgenhetService  agressoOrgenhetService;

    public OrgEnhetResolver(OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.agressoOrgenhetService = new AgressoOrgenhetService(orgEnhetStateRepository);
    }

    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch orgEnhetSearch) {
        if(orgEnhetSearch.getId() != null) {
            return orgEnhetStateRepository.findById(orgEnhetSearch.getId()).map(MappingUtils::toOrgEnhet);
        }
        if (orgEnhetSearch.getAgressoId() != null && orgEnhetSearch.getOrgNiv() != null) {
            return agressoOrgenhetService.getEnhetByAgressoIdNivaAndName(orgEnhetSearch.getAgressoId(), orgEnhetSearch.getOrgNiv(), orgEnhetSearch.getNavn());
        }
        return Optional.empty();
    }

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search) {
        final var ider = search.resolveIder();
        final var nomNivaa = search.resolveNomNivaaEnum();

        if(ider.isEmpty()){
            var agressoIdHashSet = search.getAgressoIder() != null ? new HashSet<>(search.getAgressoIder()) : new HashSet<String>();
            var agressoRequested = !agressoIdHashSet.isEmpty() || search.getOrgNiv() != null;
            if(agressoRequested){
                return agressoOrgenhetService.orgenheterByAgressoData(agressoIdHashSet, search.getOrgNiv(), search.resolveNomNivaaEnum());
            }

            return orgEnhetStateRepository.getAll().stream()
                .map(MappingUtils::toOrgEnhet)
                .filter(oe -> nomNivaa.isEmpty() || nomNivaa.contains(oe.getNomNivaa()))
                .map(OrgEnhetResult::new).toList();
        }

        final var orgenhetStateList = orgEnhetStateRepository.findByIdIn(ider);
        return orgenhetStateList.stream()
            .map(MappingUtils::toOrgEnhet)
            .filter(oe -> nomNivaa.isEmpty() || nomNivaa.contains(oe.getNomNivaa()))
            .map(OrgEnhetResult::new).toList();
    }

    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgEnhetStateRepository.findById(organisering.getId()).map(MappingUtils::toOrgEnhet).orElse(null);
    }

    private static class AgressoOrgenhetService {
        private final OrgEnhetStateRepository orgEnhetStateRepository;

        public AgressoOrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository) {
            this.orgEnhetStateRepository = orgEnhetStateRepository;
        }


        public Optional<OrgEnhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
            if (name != null) {
                return orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                    .stream()
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(comparing(OrgEnhetState::gyldigFom))
                    .map(MappingUtils::toOrgEnhet);
            }
            return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
        }


        public Optional<OrgEnhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
            return orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
                .stream()
                .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                .max(comparing(OrgEnhetState::gyldigFom))
                .map(MappingUtils::toOrgEnhet);
        }


        // agressoIdHashSet is HashSet, not Set, because we need the `HashSet::remove` method to work
        public List<OrgEnhetResult> orgenheterByAgressoData(HashSet<String> agressoIdHashSet, String orgNiv, Set<NomNivaa> nomNivaaSet){
            // if no agressoIdHashSet are specified, then we want to filter everything by the other agresso parameters
            var orgenhetStateListByAgressoIdSet = agressoIdHashSet.isEmpty() ? this.orgEnhetStateRepository.getAll() : this.orgEnhetStateRepository.findAllByAgressoIdIn(agressoIdHashSet);

            var unfilteredOrgenhetList = orgenhetStateListByAgressoIdSet.stream().map(MappingUtils::toOrgEnhet).toList();

            final var orgEnhetResults = unfilteredOrgenhetList.stream()
                .filter(e -> orgNiv == null || orgNiv.equals(e.getAgressoInfo().getAgressoOrgNiv()))
                .filter(e -> nomNivaaSet.isEmpty() || nomNivaaSet.contains(e.getNomNivaa()))
                .peek(orgEnhet -> agressoIdHashSet.remove(orgEnhet.getAgressoInfo().getAgressoId()))
                .map(OrgEnhetResult::new)
                .collect(Collectors.toList());

            var notFoundElements = agressoIdHashSet.stream().map(id -> OrgEnhetResult.notFound(id, orgNiv)).toList();
            orgEnhetResults.addAll(notFoundElements);
            return orgEnhetResults;
        }

    }
}
