package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
public class OrgEnhetResolver {

    private final OrgenhetService orgenhetService;

    OrgEnhetResolver(
        OrgenhetService orgenhetService
    ) {
        this.orgenhetService = orgenhetService;
    }

    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch search) {
        if( search.getId() != null){
            return orgenhetService.getById(search.getId()).map(MappingUtils::toOrgEnhet);
        }
        if (search.getAgressoId() != null && search.getOrgNiv() != null) {
            return orgenhetService.getEnhetByAgressoIdNivaAndName(search.getAgressoId(), search.getOrgNiv(), search.getNavn())
                    .map(MappingUtils::toOrgEnhet);
        }
        return Optional.empty();
    }

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search) {

        final var ider = search.resolveIder();
        if(ider.isEmpty()){
            return orgenheterByAgressoData(search);
        }

        final var nomNivaa = search.resolveNomNivaaEnum();

        final var mapResult = orgenhetService.getByIdBulk(ider.stream().toList());
        return mapResult.values().stream()
            .map(MappingUtils::toOrgEnhet)
            .filter(oe -> nomNivaa.isEmpty() || nomNivaa.contains(oe.getNomNivaa()))
            .map(OrgEnhetResult::new).toList();
    }

    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgenhetService.getEnhetByAgressoIdNivaAndName(organisering.getOrgEnhetNr(), organisering.getOrgNiv(), organisering.getOrgEnhetNavn())
                .map(MappingUtils::toOrgEnhet)
                .orElseThrow();
    }

    private List<OrgEnhetResult> orgenheterByAgressoData(OrgEnheterSearch search){
        Stream<IOrgenhet> result;
        final var agressoIder = search.resolveAgressoIder();
        if (agressoIder.isEmpty()) {
            result = orgenhetService.getAllEnheter().stream();
        } else {
            result = orgenhetService.findEnheterByAgressoIdIn(agressoIder).stream();
        }

        final var orgNiv = search.getOrgNiv();
        final var orgenhetkategori = search.resolveNomNivaaEnum();
        final var orgEnhetResults = result.filter(e -> orgNiv == null || orgNiv.equals(e.getOrgNiv()))
            .filter(e -> orgenhetkategori.isEmpty() || orgenhetkategori.contains(e.getNomNivaa().orElse(null)))
            .peek(iOrgenhet -> agressoIder.remove(iOrgenhet.getAgressoId()))
            .map(MappingUtils::toOrgEnhet)
            .map(OrgEnhetResult::new)
            .collect(toList());

        orgEnhetResults.addAll(agressoIder.stream().map(id -> new OrgEnhetResult(id, orgNiv)).toList());
        return orgEnhetResults;
    }

}
