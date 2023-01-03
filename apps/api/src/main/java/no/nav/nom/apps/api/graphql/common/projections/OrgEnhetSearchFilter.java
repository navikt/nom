package no.nav.nom.apps.api.graphql.common.projections;

import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgenhetFilter;
import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface OrgEnhetSearchFilter {
    GyldigTomSelection getGyldigTomSelection();

    OrgnivSelection getOrgnivSelection();

    default LuceneOrgenhetFilter toLuceneFilter() {
        var luceneGyldigTom = switch (this.getGyldigTomSelection()) {

            case IKKE_PASSERT -> LuceneOrgenhetFilter.GyldigTomSelection.IKKE_PASSERT;
            case PASSERT -> LuceneOrgenhetFilter.GyldigTomSelection.PASSERT;
            case ALLE -> LuceneOrgenhetFilter.GyldigTomSelection.ALLE;
        };

        var luceneOrgniv = switch (this.getOrgnivSelection()) {
            case ORGNIV_ORGENHET -> LuceneOrgenhetFilter.OrgnivSelection.ORGNIV_ORGENHET;
            case IKKE_ORGNIV_ORGENHET -> LuceneOrgenhetFilter.OrgnivSelection.IKKE_ORGNIV_ORGENHET;
            case ALLE -> LuceneOrgenhetFilter.OrgnivSelection.ALLE;
        };
        return new LuceneOrgenhetFilter(luceneGyldigTom, luceneOrgniv);
    }

    enum GyldigTomSelection {
        IKKE_PASSERT, // GYLDIG_TOM_FRAMTIDIG
        PASSERT, // GYLDIG_TOM_PASSERT
        ALLE
    }

    enum OrgnivSelection {
        ORGNIV_ORGENHET,
        IKKE_ORGNIV_ORGENHET,
        ALLE
    }

    static OrgEnhetSearchFilter alle() {
        return new OrgEnhetSearchFilter() {
            @Override
            public GyldigTomSelection getGyldigTomSelection() {
                return GyldigTomSelection.ALLE;
            }

            @Override
            public OrgnivSelection getOrgnivSelection() {
                return OrgnivSelection.ALLE;
            }
        };
    }
}
