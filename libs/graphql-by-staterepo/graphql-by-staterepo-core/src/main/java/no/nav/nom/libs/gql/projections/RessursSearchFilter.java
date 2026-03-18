package no.nav.nom.libs.gql.projections;

import no.nav.nom.libs.lucenewrapper.domain.LuceneRessursFilter;

public class RessursSearchFilter implements ResultLimit {

    private final StatusSelection statusSelection;
    private final SektorSelection sektorSelection;
    private final Limit limit;

    public RessursSearchFilter(StatusSelection statusSelection, SektorSelection sektorSelection, Limit limit) {
        this.statusSelection = statusSelection;
        this.sektorSelection = sektorSelection;
        this.limit = limit;
    }

    public StatusSelection getStatusSelection(){
        return this.statusSelection;
    }

    public SektorSelection getSektorSelection(){
        return this.sektorSelection;
    }

    public LuceneRessursFilter toLuceneFilter(){
        var luceneStatus = switch (this.getStatusSelection()){

            case AKTIV -> LuceneRessursFilter.StatusSelection.AKTIV;
            case INAKTIV -> LuceneRessursFilter.StatusSelection.INAKTIV;
            case ALLE -> LuceneRessursFilter.StatusSelection.ALLE;
        };

        var luceneSektor = switch(this.getSektorSelection()){
            case EKSTERN -> LuceneRessursFilter.SektorSelection.EKSTERN;
            case STATLIG -> LuceneRessursFilter.SektorSelection.STATLIG;
            case KOMMUNAL -> LuceneRessursFilter.SektorSelection.KOMMUNAL;
            case IKKE_EKSTERN -> LuceneRessursFilter.SektorSelection.IKKE_EKSTERN;
            case ALLE -> LuceneRessursFilter.SektorSelection.ALLE;
        };
        return new LuceneRessursFilter(luceneStatus,luceneSektor);
    }

    @Override
    public Limit getLimit() {
        return this.limit;
    }

    enum StatusSelection{
        AKTIV,
        INAKTIV,
        ALLE
    }

    public static RessursSearchFilter alle() {
        return new RessursSearchFilter(StatusSelection.ALLE,SektorSelection.ALLE, Limit.LIMIT_30 );
    }
}
