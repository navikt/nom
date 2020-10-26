package no.nav.nom.apps.nom.enheter.api.v1;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.enheter.api.v1.dto.EnhetRequestDTO;
import no.nav.nom.apps.nom.enheter.api.v1.dto.EnhetResponseSimple;
import no.nav.nom.apps.nom.enheter.enhet.Enhet;
import no.nav.nom.apps.nom.enheter.enhet.EnhetRepository;
import no.nav.nom.apps.nom.enheter.enhet.Organisering;
import no.nav.nom.apps.nom.enheter.enhet.OrganiseringRepository;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.nom.apps.nom.enheter.api.v1.EnhetController.BASE_URL;

@Unprotected
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(BASE_URL)
public class EnhetController {

    public static final String BASE_URL = "/api/v1/enhet";
    public static final String IKKE_FUNNET = "Fant ikke enhet med gitt nummer";

    private final EnhetRepository enhetRepository;
    private final OrganiseringRepository organiseringRepository;

    @GetMapping("{enhetsNr}")
    public EnhetRequestDTO getEnhet(@PathVariable final Long enhetsNr){
        return enhetRepository.findByEnhetsnrAndValid(enhetsNr)
                .map(this::converted).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, IKKE_FUNNET));
    }

    @GetMapping("rot")
    public EnhetRequestDTO getToppNode(){
        return converted(enhetRepository.findRotNode());
    }

    private EnhetRequestDTO converted(Enhet enhet){
        return EnhetRequestDTO.builder()
                .enhetsNr(enhet.getEnhetsnr())
                .navn(enhet.getNavn())
                .enhetsType(enhet.getEnhetstype())
                .gyldigFra(enhet.getGyldigFra())
                .gyldigTil(enhet.getGyldigTil())
                .underEnheter(
                        organiseringRepository.findByOverenhetAndValid(enhet.getEnhetsnr()).stream()
                                .map(o -> new EnhetResponseSimple(o.getUnderenhet(), enhetRepository.findByEnhetsnrAndValid(o.getUnderenhet()).get().getNavn()))
                                .collect(Collectors.toList())
                )
                .sti(buildSti(enhet.getEnhetsnr(), null))
                .build();
    }

    private List<EnhetResponseSimple> buildSti(Long enhetsNr, List<EnhetResponseSimple> sti){
        sti = sti == null ? sti = new ArrayList<>() : sti;
        Organisering enhetOver = organiseringRepository.findByUnderenhetAndValid(enhetsNr);
        if(enhetOver != null){
            buildSti(enhetOver.getOverenhet(), sti);
            sti.add(new EnhetResponseSimple(enhetOver.getOverenhet(), enhetRepository.findByEnhetsnrAndValid(enhetOver.getOverenhet()).get().getNavn()));
        }
        return sti;
    }
}
