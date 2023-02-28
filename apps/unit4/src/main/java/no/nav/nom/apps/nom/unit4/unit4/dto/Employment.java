package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@RequiredArgsConstructor
public class Employment {
    Unit4Status status = Unit4Status.AKTIV;
    String positionId;
    LocalDate startDate;
    LocalDate endDate;
    List<EmploymentRelatedValue> relatedValues;
}
