package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Optional;

@Getter
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

    public DvhRessursStilling(JsonNode node) {
        payload = node.toString();
        navident = Optional.ofNullable(node.get("NAV_ID").asText());
        stillingDatoFom = LocalDate.parse(node.get("FRA_DATO").asText());
        stillingDatoTom = LocalDate.parse(node.get("TIL_DATO").asText());

        String tmpStillingAgressoId = node.get("ORGENHET").asText();
        stillingAgressoId = Optional.ofNullable( tmpStillingAgressoId.startsWith("-") ? null : tmpStillingAgressoId);
        stillingOrgenhetNavn = node.get("ORGENHET_BESK").asText();

        String tmpRessursAgressoId = node.get("ORGENHET_PAA_RESSURS").asText();
        ressursAgressoId = Optional.ofNullable( tmpRessursAgressoId.startsWith("-") ? null : tmpRessursAgressoId);
        String tmpRessursOrgenhetNiv = node.get("ORGENHET_PAA_RESSURS_NIVA").asText();
        ressursAgressoId = Optional.ofNullable( tmpRessursOrgenhetNiv.startsWith("-") ? null : tmpRessursOrgenhetNiv);

        String tmpRessursRemedyEnehetId = node.get("REMEDY_ENHET_ID").asText();
        ressursAgressoId = Optional.ofNullable( tmpRessursRemedyEnehetId.startsWith("-") ? null : tmpRessursRemedyEnehetId);

        ressursAgressoDatoFom = LocalDate.parse(node.get("AGRESSO_DATE_FROM").asText());
        ressursAgressoDatoTom = LocalDate.parse(node.get("AGRESSO_DATE_FROM").asText());
        ressursDvhDatoFom = LocalDate.parse(node.get("DATE_FROM").asText());
        ressursDvhDatoTom = LocalDate.parse(node.get("DATE_TO").asText());
    }
}
