package no.nav.nom.skjermede.personer.consumer;

import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;

public class ConsumerRecordToSkjermetPersonMapper {
    public static SkjermetPerson consumerRecordToSkjermetPersonMapper(RessursDto record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .skjermetFom(record.getStartdato())
                .skjermetTom(record.getSluttdato())
                .tilknyttetRessurs(null)
                .build();
    }
}