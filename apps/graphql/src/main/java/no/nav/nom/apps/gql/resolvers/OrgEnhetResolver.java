package no.nav.nom.apps.gql.resolvers;

import no.nav.nom.apps.gql.MappingUtils;
import no.nav.nom.apps.gql.projections.OrgEnhetSearch;
import no.nav.nom.apps.gql.projections.OrgEnheterSearch;
import no.nav.nom.contract.nom.api.graphql.AgressoInfo;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrgEnhetResolver {

    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgEnhetResolver(OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch orgEnhetSearch) {
        if(orgEnhetSearch.getId() != null) {
            return orgEnhetStateRepository.findById(orgEnhetSearch.getId()).map(MappingUtils::toOrgEnhet);
        }
        if (orgEnhetSearch.getAgressoId() != null && orgEnhetSearch.getOrgNiv() != null) {
            return orgEnhetStateRepository.findAllByAgressoIdAndNiv(orgEnhetSearch.getAgressoIdDashOrgnivId()).stream().map(MappingUtils::toOrgEnhet).findFirst();
        }
        return Optional.empty();
    }

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search) {
        final var ider = search.resolveIder();
        final var nomNivaa = search.resolveNomNivaaEnum();

        if(ider.isEmpty()){
            var agressoRequsted = !search.getAgressoIder().isEmpty();
            if(agressoRequsted){
                // todo handle agresso IDs
                throw new RuntimeException("Not implemented");
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
}
