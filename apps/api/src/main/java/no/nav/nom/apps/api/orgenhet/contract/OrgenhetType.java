package no.nav.nom.apps.api.orgenhet.contract;

public enum OrgenhetType {
    AA("AA-reg"),
    ALS("Arbeidslivssenter"),
    ARK("Arbeidsrådgivningskontor"),
    DIR("Direktorat"),
    DOKSENTER("Dokumentsenter"),
    FP("Familie- og pensjonsytelser"),
    FYLKE("Fylkeskontor"),
    HMS_HOT("Hjelpemidler og tilrettelegging"),
    KLAGE("Klageinstans"),
    KONTROLL("Kontrollenhet"),
    KS("Kontaktsenter"),
    NAV_KTR("Nav-kontor"),
    NAY("Arbeid og ytelser"),
    OKO("Økonomi"),
    OKO_PEN("Økonomi pensjon"),
    OKO_STONAD("Økonomi stønad"),
    OKO_TJENESTE("Økonomitjeneste"),
    ROL("Rådgivende overlege"),
    TILTAK("Tiltak"),
    UTL("Oppfølging utland");

    public final String beskrivelse;

    OrgenhetType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }
}
