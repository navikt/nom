package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

public class NomNivaaMapper {

    // https://jira.adeo.no/browse/ORG-1711
    public static NomNivaa mapNivaaForTopmostWithinOrgEnhetType(OrgEnhetsType orgEnhetsType) {
        return switch (orgEnhetsType) {
            case DIREKTORAT, DIR, NAV_ARBEID_OG_YTELSER, NAV_FAMILIE_OG_PENSJONSYTELSER, KONTROLL_KONTROLLENHET,
                HJELPEMIDLER_OG_TILRETTELEGGING, NAV_KONTAKTSENTER, FYLKE -> NomNivaa.DRIFTSENHET;
            case NAV_KONTOR, TILTAK, ARBEIDSLIVSSENTER, ARBEIDSRAADGIVNING, KLAGEINSTANS -> NomNivaa.ARBEIDSOMRAADE;
            case NAV_OKONOMITJENESTE -> null;
            case null -> null;
        };
    }

    // https://jira.adeo.no/browse/ORG-1711
    public static NomNivaa mapNivaaForSecondTopmostWithinOrgEnhetType(OrgEnhetsType orgEnhetsType) {
        return switch (orgEnhetsType) {
            case NAV_OKONOMITJENESTE -> NomNivaa.DRIFTSENHET;
            case DIREKTORAT, DIR, NAV_ARBEID_OG_YTELSER, NAV_FAMILIE_OG_PENSJONSYTELSER, KONTROLL_KONTROLLENHET,
                HJELPEMIDLER_OG_TILRETTELEGGING, NAV_KONTAKTSENTER -> NomNivaa.ARBEIDSOMRAADE;
            case FYLKE, NAV_KONTOR, TILTAK, ARBEIDSLIVSSENTER, ARBEIDSRAADGIVNING, KLAGEINSTANS -> null;
            case null -> null;
        };
    }

    // https://jira.adeo.no/browse/ORG-1711
    public static NomNivaa mapNivaaForThirdTopmostWithinOrgEnhetType(OrgEnhetsType orgEnhetsType) {

        if (orgEnhetsType.equals(OrgEnhetsType.NAV_OKONOMITJENESTE)) {
            return NomNivaa.ARBEIDSOMRAADE;
        }

        return null;
    }
}
