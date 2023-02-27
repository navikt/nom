package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
@RequiredArgsConstructor
public class EmploymentRelatedValue {
    String positionId;
    Unit4Status status = Unit4Status.AKTIV;
    LocalDate dateFrom;
    LocalDate dateTo;
    Integer percentage = 100;
    boolean isDefaulted = false;
    boolean useInFixedPd = false;
    boolean useInAccountingCategories = false;
    boolean useInRateCalculation = false;
    boolean useInReport = false;
    String relationId;
    String relationName;
    String relatedValue;
}
