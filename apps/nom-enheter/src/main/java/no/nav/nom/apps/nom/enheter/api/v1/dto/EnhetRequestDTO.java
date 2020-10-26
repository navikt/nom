package no.nav.nom.apps.nom.enheter.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnhetRequestDTO {
    private Long enhetsNr;

    private String navn;

    private String enhetsType;

    private LocalDate gyldigFra;

    private LocalDate gyldigTil;

    private Map<String,Long> sti;

    private Map<String, Long> underEnheter;
}
