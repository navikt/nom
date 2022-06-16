package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder(toBuilder = true)
@Data
public class Periode {
    final LocalDate fraOgMed;
    final LocalDate tilOgMed;

    public boolean contains(LocalDate date) {
        return fraOgMed.isBefore(date.plusDays(1)) && (tilOgMed == null || tilOgMed.isAfter(date.minusDays(1)) );
    }
}
