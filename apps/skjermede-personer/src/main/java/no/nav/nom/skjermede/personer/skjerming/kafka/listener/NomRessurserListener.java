package no.nav.nom.skjermede.personer.skjerming.kafka.listener;

import lombok.AllArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Profile("!test")
public class NomRessurserListener {

    private final SkjermingLogikkKafka skjermingLogikkKafka;

    @Transactional(value = "transactionManager")
//    @KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    public void onMessage(RessursDTO ressurs) {
        skjermingLogikkKafka.handterAnsattHendelse(converted(ressurs), ressurs.getNavident(), ressurs.getRessurstype());
    }

    public static SkjermetPerson converted(final RessursDTO record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .fornavn(record.getFornavn())
                .etternavn(record.getEtternavn())
                .skjermetFra(record.getStartdato() != null ? record.getStartdato().atStartOfDay() : null)
                .skjermetTil(record.getSluttdato() != null ? record.getSluttdato().atTime(23, 59) : null)
                .build();
    }
}