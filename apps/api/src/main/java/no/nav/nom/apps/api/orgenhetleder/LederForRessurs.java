package no.nav.nom.apps.api.orgenhetleder;

import java.time.LocalDate;

public record LederForRessurs(
    String lederNavident,
    String orgEnhetId,
    LocalDate startdato,
    LocalDate sluttdato,
    boolean erDagligOppfolging
) {
    public boolean isActive() {
        return (startdato == null || !startdato.isAfter(LocalDate.now())) &&
                (sluttdato == null || !sluttdato.isBefore(LocalDate.now()));
    }
}
