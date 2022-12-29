package no.nav.nom.libs.lucenewrapper.domain;

import no.nav.nom.libs.lucenewrapper.search.QueryFilter;

public record LuceneRessursFilter(
        StatusSelection status,
        SektorSelection sektor
) implements QueryFilter<LuceneRessurs> {

    @Override
    public Class<LuceneRessurs> getEntityClass() {
        return LuceneRessurs.class;
    }

    @Override
    public Mode mode(){
        return Mode.CUSTOM;
    }

    public enum SektorSelection{
        EKSTERN,
        STATLIG,
        KOMMUNAL,
        IKKE_EKSTERN,
        ALLE,
    }

    public enum StatusSelection{
        AKTIV,
        INAKTIV,
        ALLE
    }

    public static LuceneRessursFilter alle(){
        return new LuceneRessursFilter(StatusSelection.ALLE,SektorSelection.ALLE);
    }
}
