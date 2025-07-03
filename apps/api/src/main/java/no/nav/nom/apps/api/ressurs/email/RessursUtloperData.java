package no.nav.nom.apps.api.ressurs.email;


import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record RessursUtloperData(
    String ressurNavn,
    String ressursNavident,
    LocalDate ressursSluttdato,
    String orgenhetNavn,
    String orgenhetLederNavident
) {
}
