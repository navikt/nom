package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class OrgEnhetResolver {

    private final IOrgenhetService orgenhetService;

    OrgEnhetResolver(IOrgenhetService orgenhetService) {
        this.orgenhetService = orgenhetService;
    }

    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch search) {
        if (search.getAgressoId() != null && search.getOrgNiv() != null) {
            return orgenhetService.getEnhetByAgressoIdNivaAndName(search.getAgressoId(), search.getOrgNiv(), search.getNavn())
                    .map(OrgEnhetResolver::convertToOrgEnhet);
        }
        return Optional.empty();
    }
    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgenhetService.getEnhetByAgressoIdNivaAndName(organisering.getOrgEnhetNr(), organisering.getOrgNiv(), organisering.getOrgEnhetNavn())
                .map(OrgEnhetResolver::convertToOrgEnhet)
                .orElseThrow();
    }

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search) {
        Stream<IOrgenhet> result;
        final var agressoIder = search.resolveAgressoIder();
        final var orgNiv = search.getOrgNiv();
        if (agressoIder.isEmpty()) {
            result = orgenhetService.getAllEnheter().stream();
        } else {
            result = orgenhetService.findEnheterByAgressoIdIn(agressoIder).stream();
        }

        final var orgEnhetResults = result
                .filter(e -> orgNiv == null || orgNiv.equals(e.getOrgNiv()))
                .peek(iOrgenhet -> agressoIder.remove(iOrgenhet.getAgressoId()))
                .map(OrgEnhetResolver::convertToOrgEnhet)
                .map(OrgEnhetResult::new);

        return Stream.concat(
                orgEnhetResults,
                agressoIder.stream().map(id -> new OrgEnhetResult(id, orgNiv))
        ).toList();
    }

    public static OrgEnhet convertToOrgEnhet(IOrgenhet orgenhet) {
        return OrgEnhet.builder()
                .id(orgenhet.getFid().toString())
                .agressoId(orgenhet.getAgressoId())
                .orgNiv(orgenhet.getOrgNiv())
                .navn(orgenhet.getNavn())
                .agressoOrgenhetType(orgenhet.getAgressoOrgenhetType())
                .gyldigFom(orgenhet.getGyldigFom())
                .gyldigTom(orgenhet.getGyldigTom().orElse(null))
                .build();
    }
}
