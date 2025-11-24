package no.nav.nom.libs.gql.projections;

import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgenhetFilter;

public class OrgEnhetSearchFilter implements ResultLimit {


    private final GyldigTomSelection gyldigTom;
    private final OrgnivSelection orgNivSelection;
    private final Limit limit;

    public OrgEnhetSearchFilter(GyldigTomSelection gyldigTomSelection, OrgnivSelection orgnivSelection, Limit limit) {
        this.gyldigTom = gyldigTomSelection;
        this.orgNivSelection = orgnivSelection;
        this.limit = limit;
    }

    public GyldigTomSelection getGyldigTomSelection(){
        return this.gyldigTom;
    }

    public OrgnivSelection getOrgnivSelection(){
        return this.orgNivSelection;
    }

    public LuceneOrgenhetFilter toLuceneFilter() {
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

    @Override
    public Limit getLimit() {
        return this.limit;
    }

    public enum GyldigTomSelection {
        IKKE_PASSERT, // GYLDIG_TOM_FRAMTIDIG
        PASSERT, // GYLDIG_TOM_PASSERT
        ALLE
    }

    public enum OrgnivSelection {
        ORGNIV_ORGENHET,
        IKKE_ORGNIV_ORGENHET,
        ALLE
    }

    public static OrgEnhetSearchFilter alle() {
        return new OrgEnhetSearchFilter(GyldigTomSelection.ALLE, OrgnivSelection.ALLE, Limit.LIMIT_30 );
    }
}
