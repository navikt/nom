package no.nav.nom.libs.lucenewrapper.domain;

import no.nav.nom.libs.lucenewrapper.search.QueryFilter;

public record LuceneOrgenhetFilter(
    GyldigTomSelection status,
    OrgnivSelection orgniv
) implements QueryFilter<LuceneOrgEnhet> {
    public static LuceneOrgenhetFilter alle() {
        return new LuceneOrgenhetFilter(GyldigTomSelection.ALLE,OrgnivSelection.ALLE);
    }

    @Override
    public Class<LuceneOrgEnhet> getEntityClass() {
        return LuceneOrgEnhet.class;
    }

    @Override
    public Mode mode() {
        return Mode.CUSTOM;
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
}
