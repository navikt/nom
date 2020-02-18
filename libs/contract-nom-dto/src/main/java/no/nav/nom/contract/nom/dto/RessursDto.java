package no.nav.nom.contract.nom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RessursDto {

    private String personident;

    private Ressurstype ressurstype;

    private LocalDate startdato;

    private LocalDate sluttdato;

    private Long funksjonellId;
}
