package no.nav.nom.apps.api.graphql.common.projections;

import no.nav.nom.libs.lucenewrapper.domain.LuceneRessursFilter;
import org.springframework.data.web.ProjectedPayload;

@ProjectedPayload
public interface RessursSearchFilter extends ResultLimit {
    StatusSelection getStatusSelection();

    SektorSelection getSektorSelection();

    default LuceneRessursFilter toLuceneFilter(){
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

    enum SektorSelection{
        EKSTERN,
        STATLIG,
        KOMMUNAL,
        IKKE_EKSTERN,
        ALLE,
    }

    enum StatusSelection{
        AKTIV,
        INAKTIV,
        ALLE
    }

    static RessursSearchFilter alle() {
        return new RessursSearchFilter() {
            @Override
            public StatusSelection getStatusSelection() {
                return StatusSelection.ALLE;
            }

            @Override
            public SektorSelection getSektorSelection() {
                return SektorSelection.ALLE;
            }

            @Override
            public Limit getLimit() {
                return Limit.LIMIT_30;
            }
        };
    }
}
