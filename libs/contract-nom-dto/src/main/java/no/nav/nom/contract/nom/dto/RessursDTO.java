package no.nav.nom.contract.nom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RessursDTO {

    private String personident;

    private String navident;

    private String fornavn;

    private String etternavn;

    private String epost;

    private Ressurstype ressurstype;

    private LocalDateTime startdato;

    private LocalDateTime sluttdato;

    private LocalDate opprettetdato;

    private LocalDate endretdato;
}
