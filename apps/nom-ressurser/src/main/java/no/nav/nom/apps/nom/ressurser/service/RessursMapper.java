package no.nav.nom.apps.nom.ressurser.service;

import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.dto.NyRessursRequest;
import no.nav.nom.contract.nom.dto.RessursDto;

public class RessursMapper {
    public static RessursDto RessursToRessursDto(Ressurs ressurs){
        return RessursDto.builder()
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
                .startdato(ressursRequest.getStartdato())
                .sluttdato(ressursRequest.getSluttdato())
                .build();
    }
}