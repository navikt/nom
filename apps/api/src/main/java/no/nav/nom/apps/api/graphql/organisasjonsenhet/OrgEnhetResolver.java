package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearch;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnheterSearch;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.contract.nom.OrgEnhetsKategori;
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
        final var orgenhetkategori = search.resolveOrgEnhetsKategori()
                .stream()
                .map(OrgEnhetsKategori::valueOf)
                .map(OrgEnhetResolver::from)
                .collect(Collectors.toSet());
        final var orgEnhetResults = result.filter(e -> orgNiv == null || orgNiv.equals(e.getOrgNiv()))
                .filter(e -> orgenhetkategori.isEmpty() || orgenhetkategori.contains(e.getOrgEnhetsKategori().orElse(null)))
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
                .orgEnhetsKategori(orgenhet.getOrgEnhetsKategori().map(OrgEnhetResolver::from).orElse(null))
                .build();
    }

    private static OrgEnhetsKategori from(no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori orgEnhetsKategori) {
        return switch (orgEnhetsKategori) {
            case ARBEIDSOMRAADE -> OrgEnhetsKategori.ARBEIDSOMRAADE;
            case DRIFTSENHET -> OrgEnhetsKategori.DRIFTSENHET;
            case LINJEENHET -> OrgEnhetsKategori.LINJEENHET;
        };
    }

    private static no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori from(OrgEnhetsKategori orgEnhetsKategori) {
        return switch (orgEnhetsKategori) {
            case ARBEIDSOMRAADE -> no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori.ARBEIDSOMRAADE;
            case DRIFTSENHET -> no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori.DRIFTSENHET;
            case LINJEENHET -> no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori.LINJEENHET;
        };
    }
}
