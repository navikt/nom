package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@RequiredArgsConstructor
public class RelatedValue{
    Unit4Status status = Unit4Status.AKTIV;
    String relationId;
    String relatedValue;
    Integer percentage = 100;
    LocalDateTime dateFrom;
    LocalDateTime dateTo;
}
