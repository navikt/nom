package no.nav.nom.apps.nom.enheter.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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

    private List<EnhetResponseSimple> sti;

    private List<EnhetResponseSimple> underEnheter;
}
