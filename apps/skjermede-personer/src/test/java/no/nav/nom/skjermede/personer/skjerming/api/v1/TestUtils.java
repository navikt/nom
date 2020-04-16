package no.nav.nom.skjermede.personer.skjerming.api.v1;

import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class TestUtils {

    public static final String PERSONIDENT = "12345678910";
    public static final String NAVIDENT = "A000000";


    public static SkjermetPerson skjermetPerson() {
        return SkjermetPerson.builder()
                .personident(PERSONIDENT)
                .skjermetFra(LocalDateTime.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .build();
    }

    public static ResponseEntity<RessursDTO> ressurs() {
        return new ResponseEntity<>(RessursDTO.builder()
                .personident(PERSONIDENT)
                .navident(NAVIDENT)
                .sluttdato(LocalDateTime.now())
                .build(), HttpStatus.OK);
    }
}
