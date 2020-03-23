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

    private String navident;

    private Ressurstype ressurstype;

    private String fornavn;

    private String etternavn;

    private LocalDate startdato;

    private LocalDate sluttdato;
}
