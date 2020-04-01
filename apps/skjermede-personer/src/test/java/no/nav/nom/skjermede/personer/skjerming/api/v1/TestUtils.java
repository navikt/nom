package no.nav.nom.skjermede.personer.skjerming.api.v1;

import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestUtils {

    public static final String PERSONIDENT = "12345678910";
    public static final String NAVIDENT = "A000000";


    public static SkjermetPerson skjermetPerson() {
        return SkjermetPerson.builder()
                .personident(PERSONIDENT)
                .skjermetFom(LocalDate.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .build();
    }

    public static ResponseEntity<RessursDto> ressurs() {

        return new ResponseEntity<RessursDto>(RessursDto.builder()
                .personident(PERSONIDENT)
                .navident(NAVIDENT)
                .sluttdato(LocalDate.now())
                .build(), HttpStatus.OK);
    }
}
