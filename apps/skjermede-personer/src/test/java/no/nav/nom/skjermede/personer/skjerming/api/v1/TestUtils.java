package no.nav.nom.skjermede.personer.skjerming.api.v1;

import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonRequestDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    public static final String PERSONIDENT = "12345678910";
    public static final String NAVIDENT = "A000000";

    public static SkjermetPerson skjermetPerson() {
        return SkjermetPerson.builder()
                .personident(PERSONIDENT)
                .navident(NAVIDENT)
                .skjermetFom(LocalDate.now())
                .opprettetDato(LocalDateTime.now())
                .endretDato(LocalDateTime.now())
                .build();
    }

    public static SkjermetPersonRequestDTO skjermetPersonRequest() {
        return SkjermetPersonRequestDTO.builder()
                .navident(NAVIDENT)
                .skjermetFom(LocalDate.now())
                .build();
    }

    public static RessursDto ressurs() {
        return RessursDto.builder()
                .personident(PERSONIDENT)
                .navident(NAVIDENT)
                .sluttdato(LocalDate.now())
                .build();
    }

    public static List<SkjermetPerson> skjermetPersonList(boolean filled) {
        List<SkjermetPerson> list = new ArrayList<>();
        if (filled) {
            list.add(skjermetPerson());
        }
        return list;
    }
}
