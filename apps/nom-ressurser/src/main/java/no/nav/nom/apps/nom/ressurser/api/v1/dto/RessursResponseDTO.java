package no.nav.nom.apps.nom.ressurser.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.contract.nom.dto.Ressurstype;

import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RessursResponseDTO {

    private String personident;
    private String navident;
    private String fornavn;
    private String etternavn;
    private String epost;
    private Ressurstype ressurstype;
    private Boolean permisjon;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sluttdato;

    public static RessursResponseDTO fromEntity(final Ressurs ressurs) {
        return builder()
                .personident(ressurs.getPersonident())
                .navident(ressurs.getNavident())
                .fornavn(ressurs.getFornavn())
                .etternavn(ressurs.getEtternavn())
                .epost(ressurs.getEpost())
                .ressurstype(ressurs.getRessurstype())
                .permisjon(ressurs.getPermisjon())
                .startdato(ressurs.getStartdato())
                .sluttdato(ressurs.getSluttdato())
                .build();
    }
}
