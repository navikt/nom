package no.nav.nom.apps.nom.ressurser.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.contract.nom.dto.Ressurstype;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RessursResponseDTO {

    @JsonProperty
    private String personident;

    @JsonProperty
    private String navident;

    @JsonProperty
    private String fornavn;

    @JsonProperty
    private String etternavn;

    @JsonProperty
    private String epost;

    @JsonProperty
    private Ressurstype ressurstype;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sluttdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate opprettetdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endretdato;

    public static RessursResponseDTO fromEntity(final Ressurs ressurs) {
        return builder()
                .personident(ressurs.getPersonident())
                .navident(ressurs.getNavident())
                .fornavn(ressurs.getFornavn())
                .etternavn(ressurs.getEtternavn())
                .epost(ressurs.getEpost())
                .ressurstype(ressurs.getRessurstype())
                .startdato(ressurs.getStartdato())
                .sluttdato(ressurs.getSluttdato())
                .opprettetdato(ressurs.getOpprettetDato())
                .endretdato(ressurs.getEndretDato())
                .build();
    }
}
