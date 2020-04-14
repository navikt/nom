package no.nav.nom.skjermede.personer.skjerming;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@AllArgsConstructor
@Slf4j
@Profile("!test")
public class NomRessurserListener {

    private final SkjermingLogikkKafka skjermingLogikkKafka;

    @Transactional
    //@KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(RessursDto ressurs) {
        if(ressurs.getRessurstype() == Ressurstype.INTERN) {
            skjermingLogikkKafka.handterAnsattHendelse(converted(ressurs), ressurs.getNavident());
        }
    }

    public static SkjermetPerson converted(RessursDto record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .fornavn(record.getFornavn())
                .etternavn(record.getEtternavn())
                .skjermetFom(record.getStartdato())
                .skjermetTom(record.getSluttdato())
                .tilknyttetRessurs(null)
                .build();
    }
}