package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Value
@Builder
public class DvhRessursStilling {
    Optional<String> navident;
    LocalDate stillingDatoFom;                          // DVH sin HRRES_STILLINGER.FRA_DATO            Fra dato for brekkpunktet for dette datapunktet
    LocalDate stillingDatoTom;                          // DVH sin HRRES_STILLINGER.TIL_DATO            Fra dato for brekkpunktet for dette datapunktet
    Optional<String> stillingAgressoId;                 // DVH sin HRRES_STILLINGER.ORGENHET            Agresso id som unit4-stillingen er knyttet til i perioden for FRA/TIL_DATO.
    Optional<String> stillingAnsettelsesTypeBesk;       // DVH sin HRRES_STILLINGER.ANSETELSESTYPE_BESK  Beskrivelse av ansettelsestype
                                                //                                              Kan være NULL/TOM/MANGLENDE
    String stillingOrgenhetNavn;
    Optional<String> ressursAgressoId;                  // DVH sin HRRES_RESSURS.ORGENHET_PAA_RESSURS   Agresso id som unit4-ressursen er knyttet til NÅ
                                                //                                              Kan være NULL/TOM/MANGLENDE
    Optional<String> ressursOrgenhetNiv;
    Optional<String> ressursRemedyEnhetId;
    Optional<String> stillingRessursAgressoId;  // DVH sin HRRES_STILLINGER.ORGENHET_PAA_RESSURS   Agresso id som unit4-ressursen er knyttet til i perioden for FRA/TIL_DATO
                                                //                                                 Kan være NULL/TOM/MANGLENDE

    LocalDate ressursAgressoDatoFom;            // DVH sin HRRES_RESSURS.AGRESSO_DATE_FROM         Fra dato på ressurs i Unit4  - NÅ
    LocalDate ressursAgressoDatoTom;            // DVH sin HRRES_RESSURS.AGRESSO_DATE_TO           Til dato på ressurs i Unit4  - NÅ
    LocalDate ressursDvhDatoFom;                // DVH sin HRRES_RESSURS.DATE_FROM                 Fra dato på ressurs i dvh    - NÅ
    LocalDate ressursDvhDatoTom;                // DVH sin HRRES_RESSURS.DATE_TO                   Til dato på ressurs i dvh    - NÅ
    String payload;

    // Mulige Dvh koder for NULL/TOM/MANGLENDE verdi
    // -1 = "Ukjent" dvs koden som er observert matcher ikke kodeverket.
    // -2 = Kommer snart. Vi har observert en gyldig kode, men har ikke mottatt fulle dimensjonsopplysninger ennå. (F.eks mangler i hierarkiene over en orgenhet)
    // -3 = Ikke aktuell -- Helt ok med NULL i kilden for dette feltet kan være blankt. F.eks Fraværsårsak for en person som er på jobb...
    // -4 = Manlger - Feltet er blankt i kilden, men det burde ha vært en verdi der.
    // -5 = Skjult - Kode 6 / 7 eller annet grunnlag for å skjule denne opplysningen.

    private static Optional<String> getNullableOrErrorStringNode(JsonNode json, String field_name) {
        JsonNode node = json.get(field_name);
        return (node.isNull() || node.asText().startsWith("-")) ?
            Optional.empty() :
            Optional.of(node.asText());
    }

    public Sektor getSektor() {
        var ansettelsesBeskTypeValue = this.getStillingAnsettelsesTypeBesk().orElse("");
        return switch (ansettelsesBeskTypeValue) {
            case "Fast", "Midlertidig karakter § 9a" -> Sektor.NAV_STATLIG;
            default -> null;
        };
    }

    public Optional<Tilknytningtype> getTilknytningtype() {
        var ansettelsesBeskTypeValue = this.getStillingAnsettelsesTypeBesk().orElse("");
        return switch (ansettelsesBeskTypeValue) {
            case "Fast" -> Optional.of(Tilknytningtype.FAST);
            case "Midlertidig karakter § 9a" -> Optional.of(Tilknytningtype.MIDLERTIDIG);
            case "Uforutsett behov § 9e" -> Optional.of(Tilknytningtype.UFORUTSETT_BEHOV);
            case "Vikar § 9b" -> Optional.of(Tilknytningtype.VIKAR);
            case "Utdanningsstillinger § 9d" -> Optional.of(Tilknytningtype.UTDANNINGSSTILLINGER);
            case "Praksisarbeid § 9c" -> Optional.of(Tilknytningtype.PRAKSISARBEID);
            default -> Optional.empty();
        };
    }

    public static DvhRessursStilling fromOrdsRessursStilling(JsonNode json) {
        var navident = getNullableOrErrorStringNode(json, "nav_id");

        var dvhRessursStillingBuilder = DvhRessursStilling.builder()
            .payload(json.toPrettyString())
            .navident(navident);

        if (navident.isPresent()) {
            dvhRessursStillingBuilder
                .stillingDatoFom(localDateOf(json.get("fra_dato").asText()))
                .stillingDatoTom(localDateOf(json.get("til_dato").asText()))
                .stillingAgressoId(getNullableOrErrorStringNode(json, "orgenhet"))
                .stillingOrgenhetNavn(json.get("orgenhet_besk").asText())
                .ressursAgressoId(getNullableOrErrorStringNode(json, "orgenhet_paa_ressurs"))
                .ressursOrgenhetNiv(getNullableOrErrorStringNode(json, "orgenhet_paa_ressurs_niva"))
                .ressursRemedyEnhetId(getNullableOrErrorStringNode(json, "remedy_enhet_id"))
                .stillingRessursAgressoId(getNullableOrErrorStringNode(json, "stilling_orgenhet_paa_ressurs"))
                .stillingAnsettelsesTypeBesk(getNullableOrErrorStringNode(json, "ansettelsestype_besk"))
                .ressursAgressoDatoFom(localDateOf(json.get("agresso_date_from").asText()))
                .ressursAgressoDatoTom(localDateOf(json.get("agresso_date_to").asText()))
                .ressursDvhDatoFom(localDateOf(json.get("date_from").asText()))
                .ressursDvhDatoTom(localDateOf(json.get("date_to").asText()));
        }

        return dvhRessursStillingBuilder.build();
    }

    public static LocalDate localDateOf(String date) {
        return LocalDate.ofInstant(Instant.parse(date), ZoneId.of("Europe/Oslo")); // timstamped datoer fra ORDS er i Oslo tid levert som UTC, E.g datoen 2025-12-31 leveres som 2025-12-30T23:00:00Z
    }
}
