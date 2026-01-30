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

import java.util.ArrayList;
import java.util.Collections;
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
        if (orgEnhetSearch.getAgressoId() != null && (orgEnhetSearch.getOrgNiv() != null || orgEnhetSearch.agressoOrgNiv() != null)) {
            return agressoOrgenhetService.getEnhetByAgressoIdNivaAndName(
                orgEnhetSearch.getAgressoId(),
                orgEnhetSearch.agressoOrgNiv() != null ? orgEnhetSearch.agressoOrgNiv() : orgEnhetSearch.getOrgNiv(), // orgNiv is deprecated
                orgEnhetSearch.getNavn()
            );
        }
        return Optional.empty();
    }

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search) {
        final var ider = search.resolveIder();
        final var nomNivaa = search.resolveNomNivaaEnum();

        if(ider.isEmpty()){
            Set<String> agressoIds = search.getAgressoIder() != null ? search.getAgressoIder() : Collections.emptySet();
            var agressoRequested = !agressoIds.isEmpty() || search.getOrgNiv() != null || search.agressoOrgNiv() != null;
            if(agressoRequested){
                return agressoOrgenhetService.orgenheterByAgressoData(
                    agressoIds,
                    search.agressoOrgNiv() != null ? search.agressoOrgNiv() : search.getOrgNiv(),   // orgNiv is deprecated
                    search.resolveNomNivaaEnum());
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

        public List<OrgEnhetResult> orgenheterByAgressoData(Set<String> agressoIds, String orgNiv, Set<NomNivaa> nomNivaaSet){
            // if no agressoIdHashSet are specified, then we want to filter everything by the other agresso parameters
            var orgenhetStateListByAgressoIdSet = agressoIds.isEmpty() ? this.orgEnhetStateRepository.getAll() : this.orgEnhetStateRepository.findAllByAgressoIdIn(agressoIds);

            var filteredOrgenhetStateList = orgenhetStateListByAgressoIdSet.stream()
                .filter(e -> orgNiv == null || orgNiv.equals(e.agressoOrgNivaa()))
                .filter(e -> nomNivaaSet.isEmpty() || nomNivaaSet.contains(e.nomNivaa()))
                .toList();

            var foundAgressoIds = filteredOrgenhetStateList.stream().map(OrgEnhetState::agressoId).collect(Collectors.toSet());

            final var orgEnhetResults = new ArrayList<>(filteredOrgenhetStateList.stream()
                .map(MappingUtils::toOrgEnhet)
                .map(OrgEnhetResult::new)
                .toList());

            var notFoundElements = agressoIds.stream()
                .filter(id -> !foundAgressoIds.contains(id))
                .map(id -> OrgEnhetResult.notFound(id, orgNiv))
                .toList();

            orgEnhetResults.addAll(notFoundElements);
            return orgEnhetResults;
        }

    }
}
