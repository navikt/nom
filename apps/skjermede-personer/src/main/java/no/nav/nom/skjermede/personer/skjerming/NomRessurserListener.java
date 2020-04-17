package no.nav.nom.skjermede.personer.skjerming;

import lombok.AllArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@AllArgsConstructor
@Profile("!test")
public class NomRessurserListener {

    private final SkjermingLogikkKafka skjermingLogikkKafka;

    @Transactional
    //@KafkaListener(topics = "aapen-nom-ressurs-v1", groupId = "skjermede-personer")
    void onMessage(RessursDTO ressurs) {
        if(ressurs.getRessurstype() == Ressurstype.INTERN) {
            skjermingLogikkKafka.handterAnsattHendelse(converted(ressurs), ressurs.getNavident());
        }
    }

    public static SkjermetPerson converted(final RessursDTO record){
        return SkjermetPerson.builder()
                .personident(record.getPersonident())
                .fornavn(record.getFornavn())
                .etternavn(record.getEtternavn())
                .skjermetFra(record.getStartdato().atStartOfDay())
                .skjermetTil(record.getSluttdato().atTime(23, 59))
                .build();
    }
}