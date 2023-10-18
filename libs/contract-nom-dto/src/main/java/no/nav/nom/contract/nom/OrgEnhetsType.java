package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum OrgEnhetsType {
    ARBEIDSLIVSSENTER,
    NAV_ARBEID_OG_YTELSER,
    ARBEIDSRAADGIVNING,
    DIREKTORAT,
    DIR, // TODO: DIR vs Direktorat
    FYLKE,
    NAV_FAMILIE_OG_PENSJONSYTELSER,
    HJELPEMIDLER_OG_TILRETTELEGGING,
    KLAGEINSTANS,
    NAV_KONTAKTSENTER,
    KONTROLL_KONTROLLENHET,
    NAV_KONTOR,
    TILTAK,
    NAV_OKONOMITJENESTE;

}
