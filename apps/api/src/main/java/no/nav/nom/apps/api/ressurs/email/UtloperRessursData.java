package no.nav.nom.apps.api.ressurs.email;


import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;

@Builder(toBuilder = true)
public record UtloperRessursData(
    String ressurNavn,
    String ressursNavident,
    LocalDate ressursSluttdato,

    // orgenhetData inneholder orgenhetNavn og orgenhetLederNavident
    Map<String, String> orgenhetData
) {
}
