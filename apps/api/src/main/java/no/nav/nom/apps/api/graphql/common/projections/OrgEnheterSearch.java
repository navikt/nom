package no.nav.nom.apps.api.graphql.common.projections;

import no.nav.nom.contract.nom.NomNivaa;
import org.springframework.data.web.ProjectedPayload;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * Projected payload for binding the graphql input type OrgEnheterSearch
 * Use resolveAgressoIder() to resolve the list of agressoIder to search for.
 */
@ProjectedPayload
public interface OrgEnheterSearch {
    OrgEnheterSearch EMPTY_SEARCH = emptyOrgEnheterSearch();

    Set<String> getAgressoIder();

    Set<String> getOrgEnhetsKategori();

    String getOrgNiv();

    Set<String> getIder();

    default Set<String> resolveAgressoIder() {
        if (getAgressoIder() == null) {
            return emptySet();
        }
        return getAgressoIder();
    }

    default Set<String> resolveNomNivaa() {
        if (getOrgEnhetsKategori() == null) {
            return emptySet();
        }
        return getOrgEnhetsKategori();
    }

    default Set<NomNivaa> resolveNomNivaaEnum() {
        return this.resolveNomNivaa()
            .stream()
            .map(NomNivaa::valueOf)
            .collect(Collectors.toSet());
    }

    default Set<String> resolveIder() {
        if (getIder() == null){
            return emptySet();
        }
        return getIder();
    }

    static OrgEnheterSearch emptyOrgEnheterSearch() {
        return new OrgEnheterSearch() {
            @Override
            public Set<String> getAgressoIder() {
                return emptySet();
            }

            @Override
            public Set<String> getOrgEnhetsKategori() {
                return emptySet();
            }

            @Override
            public String getOrgNiv() {
                return null;
            }

            @Override
            public Set<String> getIder() {
                return emptySet();
            }
        };
    }
}
