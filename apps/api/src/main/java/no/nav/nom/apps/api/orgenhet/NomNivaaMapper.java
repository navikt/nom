package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.util.Optional;

public class NomNivaaMapper {

    // https://jira.adeo.no/browse/ORG-1711
    public static Optional<NomNivaa> mapNivaaForTopmostWithinOrgEnhetType(OrgEnhetsType orgEnhetsType) {
        return switch (orgEnhetsType) {
            case DIREKTORAT, DIR, NAV_ARBEID_OG_YTELSER, NAV_FAMILIE_OG_PENSJONSYTELSER, KONTROLL_KONTROLLENHET,
                HJELPEMIDLER_OG_TILRETTELEGGING, NAV_KONTAKTSENTER, FYLKE, KLAGEINSTANS -> Optional.of(NomNivaa.DRIFTSENHET);
            case NAV_KONTOR, TILTAK, ARBEIDSLIVSSENTER, ARBEIDSRAADGIVNING -> Optional.of(NomNivaa.ARBEIDSOMRAADE);
            case NAV_OKONOMITJENESTE -> Optional.empty();
            case null -> Optional.empty();
        };
    }

    // https://jira.adeo.no/browse/ORG-1711
    public static Optional<NomNivaa> mapNivaaForSecondTopmostWithinOrgEnhetType(OrgEnhetsType orgEnhetsType) {
        return switch (orgEnhetsType) {
            case NAV_OKONOMITJENESTE -> Optional.of(NomNivaa.DRIFTSENHET);
            case DIREKTORAT, DIR, NAV_ARBEID_OG_YTELSER, NAV_FAMILIE_OG_PENSJONSYTELSER, KONTROLL_KONTROLLENHET,
                HJELPEMIDLER_OG_TILRETTELEGGING, NAV_KONTAKTSENTER, KLAGEINSTANS -> Optional.of(NomNivaa.ARBEIDSOMRAADE);
            case FYLKE, NAV_KONTOR, TILTAK, ARBEIDSLIVSSENTER, ARBEIDSRAADGIVNING -> Optional.empty();
            case null -> Optional.empty();
        };
    }

    // https://jira.adeo.no/browse/ORG-1711
    public static Optional<NomNivaa> mapNivaaForThirdTopmostWithinOrgEnhetType(OrgEnhetsType orgEnhetsType) {

        if (orgEnhetsType.equals(OrgEnhetsType.NAV_OKONOMITJENESTE)) {
            return Optional.of(NomNivaa.ARBEIDSOMRAADE);
        }

        return Optional.empty();
    }
}
