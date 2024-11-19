package no.nav.nom.apps.rdbmsdata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.rdbmsdata.repository.MasteredRessurs;
import no.nav.nom.apps.rdbmsdata.repository.MasteredRessursRepository;
import no.nav.nom.apps.rdbmsdata.repository.Organisering;
import no.nav.nom.apps.rdbmsdata.repository.OrganiseringRepository;
import no.nav.nom.apps.rdbmsdata.repository.Orgenhet;
import no.nav.nom.apps.rdbmsdata.repository.OrgenhetRepository;
import no.nav.nom.apps.rdbmsdata.repository.Orgtilknytning;
import no.nav.nom.apps.rdbmsdata.repository.OrgtilknytningRepository;
import no.nav.nom.apps.rdbmsdata.repository.Ressurs;
import no.nav.nom.apps.rdbmsdata.repository.RessursRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class KafkaListeners {
    private final RessursRepository ressursRepository;
    private final OrganiseringRepository organiseringRepository;
    private final OrgenhetRepository orgenhetRepository;
    private final OrgtilknytningRepository orgtilknytningRepository;
    private final MasteredRessursRepository masteredRessursRepository;

    public KafkaListeners(RessursRepository ressursRepository, OrganiseringRepository organiseringRepository,
                          OrgenhetRepository orgenhetRepository,
                          OrgtilknytningRepository orgtilknytningRepository,
                          MasteredRessursRepository masteredRessursRepository) {
        this.ressursRepository = ressursRepository;
        this.organiseringRepository = organiseringRepository;
        this.orgenhetRepository = orgenhetRepository;
        this.orgtilknytningRepository = orgtilknytningRepository;
        this.masteredRessursRepository = masteredRessursRepository;
    }

    @KafkaListener(
        containerFactory = "ressursStateKafkaListenerContainerFactory",
        topics = "${app.kafka.ressurs.state-topic}"
    )
    public void onRessursState(ConsumerRecords<String, RessursState> records) {
        records.forEach(record -> {
            if (record.value() != null) {
                ressursRepository.save(Ressurs.from(record.value()));
            } else {
                ressursRepository.delete(new Ressurs(record.key()));
            }
        });
    }

    @KafkaListener(
        containerFactory = "orgenhetStateKafkaListenerContainerFactory",
        topics = "${app.kafka.orgenhet.state-topic}"
    )
    public void onOrgenhetState(ConsumerRecords<String, OrgEnhetState> records) {
        records.forEach(record -> {
            if (record.value() != null) {
                orgenhetRepository.save(Orgenhet.from(record.value()));
            } else {
                orgenhetRepository.delete(new Orgenhet(record.key()));
            }
        });
    }

    @KafkaListener(
        containerFactory = "organiseringStateKafkaListenerContainerFactory",
        topics = "${app.kafka.organisering.state-topic}"
    )
    public void onOrganiseringState(ConsumerRecords<String, OrganiseringState> records) {
        records.forEach(record -> {
            if (record.value() != null) {
                organiseringRepository.save(Organisering.from(record.value()));
            } else {
                organiseringRepository.delete(new Organisering(record.key()));
            }
        });
    }

    @KafkaListener(
        containerFactory = "orgtilknytningStateKafkaListenerContainerFactory",
        topics = "${app.kafka.orgtilknytning.state-topic}"
    )
    public void onOrgtilknytningState(ConsumerRecords<String, OrgtilknytningState> records) {
        records.forEach(record -> {
            if (record.value() != null) {
                orgtilknytningRepository.save(Orgtilknytning.from(record.value()));
            } else {
                orgtilknytningRepository.delete(new Orgtilknytning(record.key()));
            }
        });
    }

    @KafkaListener(
        containerFactory = "nomMasteredRessursKafkaListenerContainerFactory",
        topics = "${app.kafka.mastered-ressurs.evtsrc-topic}"
    )
    public void onMasteredRessursEvtsrc(ConsumerRecords<String, LocalDateTime> records) {
        records.forEach(record -> {
            var masteredRessurs = new MasteredRessurs(record.key(), record.value());
            if (record.value() != null) {
                masteredRessursRepository.save(masteredRessurs);
            } else {
                masteredRessursRepository.delete(masteredRessurs);
            }
        });
    }
}

