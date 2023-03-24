package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

@Value
@Builder
@AllArgsConstructor
public class DvhRessursStilling {
    Optional<String> navident;
    LocalDate stillingDatoFom;                  // DVH sin HRRES_STILLINGER.FRA_DATO            Fra dato for brekkpunktet for dette datapunktet
    LocalDate stillingDatoTom;                  // DVH sin HRRES_STILLINGER.TIL_DATO            Fra dato for brekkpunktet for dette datapunktet
    Optional<String> stillingAgressoId;         // DVH sin HRRES_STILLINGER.ORGENHET            Agresso id som unit4-stillingen er knyttet til i perioden for FRA/TIL_DATO.
                                                //                                              Kan være NULL/TOM/MANGLENDE
    String stillingOrgenhetNavn;
    Optional<String> ressursAgressoId;          // DVH sin HRRES_RESSURS.ORGENHET_PAA_RESSURS   Agresso id som unit4-ressursen er knyttet til NÅ
                                                //                                              Kan være NULL/TOM/MANGLENDE
    Optional<String> ressursOrgenhetNiv;
    Optional<String> ressursRemedyEnehetId;
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

    public DvhRessursStilling(JsonNode json) {
        payload = json.toString();

        navident = getNullableOrErrorStringNode(json, "NAV_ID");

        if (navident.isEmpty()) {
            // Run along - nothing to see here....
            stillingDatoFom = null;
            stillingDatoTom = null;
            stillingAgressoId = null;
            stillingOrgenhetNavn = null;
            ressursAgressoId = null;
            ressursOrgenhetNiv = null;
            ressursRemedyEnehetId = null;
            stillingRessursAgressoId = null;
            ressursAgressoDatoFom = null;
            ressursAgressoDatoTom = null;
            ressursDvhDatoFom = null;
            ressursDvhDatoTom = null;
            return;
        }

        stillingDatoFom = LocalDate.parse(json.get("FRA_DATO").asText());
        stillingDatoTom = LocalDate.parse(json.get("TIL_DATO").asText());

        stillingAgressoId = getNullableOrErrorStringNode(json, "ORGENHET");
        stillingOrgenhetNavn = json.get("ORGENHET_BESK").asText();

        ressursAgressoId = getNullableOrErrorStringNode(json, "ORGENHET_PAA_RESSURS");
        ressursOrgenhetNiv = getNullableOrErrorStringNode(json, "ORGENHET_PAA_RESSURS_NIVA");

        ressursRemedyEnehetId = getNullableOrErrorStringNode(json, "REMEDY_ENHET_ID");

        stillingRessursAgressoId = getNullableOrErrorStringNode(json, "STILLING_ORGENHET_PAA_RESSURS");

        ressursAgressoDatoFom = LocalDate.parse(json.get("AGRESSO_DATE_FROM").asText());
        ressursAgressoDatoTom = LocalDate.parse(json.get("AGRESSO_DATE_TO").asText());
        ressursDvhDatoFom = LocalDate.parse(json.get("DATE_FROM").asText());
        ressursDvhDatoTom = LocalDate.parse(json.get("DATE_TO").asText());
    }

    private Optional<String> getNullableOrErrorStringNode(JsonNode json, String field_name) {
        JsonNode node = json.get(field_name);
        return (node.isNull() || node.asText().startsWith("-")) ?
            Optional.empty() :
            Optional.of(node.asText());
    }

    private Optional<LocalDate> getNullableDateNode(JsonNode json, String field_name) {
        JsonNode node = json.get(field_name);
        return (node.isNull() || node.asText().startsWith("-")) ?
            Optional.empty() :
            Optional.of(LocalDate.parse(node.asText()));
    }
}
