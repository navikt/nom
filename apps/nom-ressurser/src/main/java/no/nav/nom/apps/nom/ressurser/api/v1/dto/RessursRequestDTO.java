package no.nav.nom.apps.nom.ressurser.api.v1.dto;

import lombok.Getter;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Getter
public class RessursRequestDTO {
    private String personident;
    private String navident;
    private String fornavn;
    private String etternavn;
    private String epost;
    private Ressurstype ressurstype;
    private Boolean permisjon;
    private String startdato;
    private String sluttdato;

    public static Ressurs toEntity(RessursRequestDTO ressursDTO) {
        return Ressurs.builder()
                .personident(ressursDTO.getPersonident())
                .navident(ressursDTO.getNavident())
                .fornavn(ressursDTO.getFornavn())
                .etternavn(ressursDTO.getEtternavn())
                .epost(ressursDTO.getEpost())
                .ressurstype(ressursDTO.getRessurstype())
                .permisjon(ressursDTO.getPermisjon())
                .startdato(LocalDate.parse(ressursDTO.getStartdato()))
                .sluttdato(ressursDTO.getSluttdato() != null ? LocalDate.parse(ressursDTO.getSluttdato()) : null)
                .build();
    }
}
