package no.nav.nom.apps.nom.ressurser.service;

import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.provider.rs.NyRessursRequest;
import no.nav.nom.contract.nom.dto.RessursDto;

import java.time.LocalDate;

public class RessursMapper {
    public static RessursDto RessursToRessursDto(Ressurs ressurs){
        return RessursDto.builder()
                .funksjonellId(ressurs.getFunksjonellId())
                .personident(ressurs.getPersonident())
                .ressurstype(ressurs.getRessurstype())
                .startdato(ressurs.getStartdato())
                .sluttdato(ressurs.getSluttdato())
                .build();
    }

    public static Ressurs RessursRequestToRessurs(NyRessursRequest ressursRequest){
        return Ressurs.builder()
                .personident(ressursRequest.getPersonident())
                .ressurstype(ressursRequest.getRessurstype())
                .startdato(LocalDate.parse(ressursRequest.getStartdato()))
                .sluttdato(ressursRequest.getSluttdato() == null ? null : LocalDate.parse(ressursRequest.getSluttdato()))
                .build();
    }
}