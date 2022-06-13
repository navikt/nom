package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class Periode {
    LocalDate fraOgMed;
    LocalDate tilOgMed;

    public boolean contains(LocalDate date) {
        return fraOgMed.isBefore(date.plusDays(1)) && (tilOgMed == null || tilOgMed.isAfter(date.minusDays(1)) );
    }
}
