package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Orgenhetskategori;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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

    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch search) {
        Stream<IOrgenhet> result;
        final var agressoIder = search.resolveAgressoIder();
        if (agressoIder.isEmpty()) {
            result = orgenhetService.getAllEnheter().stream();
        } else {
            result = orgenhetService.findEnheterByAgressoIdIn(agressoIder).stream();
        }

        final var orgNiv = search.getOrgNiv();
        final var orgenhetkategori = search.resolveOrgenhetskategori().stream().map(Orgenhetskategori::valueOf).map(OrgEnhetResolver::from).collect(Collectors.toSet());
        final var orgEnhetResults = result
                .filter(e -> orgNiv == null || orgNiv.equals(e.getOrgNiv()))
                .filter(e -> orgenhetkategori.isEmpty() || orgenhetkategori.contains(e.getOrgenhetskategori().orElse(null)))
                .peek(iOrgenhet -> agressoIder.remove(iOrgenhet.getAgressoId()))
                .map(OrgEnhetResolver::convertToOrgEnhet)
                .map(OrgEnhetResult::new)
                .collect(toList());

        orgEnhetResults.addAll(agressoIder.stream().map(id -> new OrgEnhetResult(id, orgNiv)).toList());

        return orgEnhetResults;
    }

    public OrgEnhet orgEnhet(Organisering organisering) {
        return orgenhetService.getEnhetByAgressoIdNivaAndName(organisering.getOrgEnhetNr(), organisering.getOrgNiv(), organisering.getOrgEnhetNavn())
                .map(OrgEnhetResolver::convertToOrgEnhet)
                .orElseThrow();
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
                .orgenhetskategori(
                        orgenhet.getOrgenhetskategori()
                                .map(OrgEnhetResolver::from)
                                .orElse(null)
                )
                .build();
    }


    private static Orgenhetskategori from(no.nav.nom.apps.api.orgenhet.contract.Orgenhetskategori orgenhetskategori){
        return switch (orgenhetskategori){
            case ARBEIDSOMRAADE -> Orgenhetskategori.ARBEIDSOMRAADE;
            case DRIFTSENHET -> Orgenhetskategori.DRIFTSENHET;
            case LINJEENHET -> Orgenhetskategori.LINJEENHET;
        };
    }

    private static no.nav.nom.apps.api.orgenhet.contract.Orgenhetskategori from(Orgenhetskategori orgenhetskategori){
        return switch (orgenhetskategori){
            case ARBEIDSOMRAADE -> no.nav.nom.apps.api.orgenhet.contract.Orgenhetskategori.ARBEIDSOMRAADE;
            case DRIFTSENHET -> no.nav.nom.apps.api.orgenhet.contract.Orgenhetskategori.DRIFTSENHET;
            case LINJEENHET -> no.nav.nom.apps.api.orgenhet.contract.Orgenhetskategori.LINJEENHET;
        };
    }
}
