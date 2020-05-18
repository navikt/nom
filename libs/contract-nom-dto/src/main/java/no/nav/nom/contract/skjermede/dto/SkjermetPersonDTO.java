package no.nav.nom.contract.skjermede.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SkjermetPersonDTO {

    private LocalDateTime skjermetFra;

    private LocalDateTime skjermetTil;
}
