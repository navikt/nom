package no.nav.nom.apps.nom.ressurser.producer;

import lombok.AllArgsConstructor;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class KafkaScheduler {

    private final RessursRepository ressursRepository;
    private final KafkaProducer kafkaProducer;
    private final LeaderElection leaderElection;

    //@Scheduled(fixedDelay = 5000)
    public void produce(){
        if(leaderElection.isLeader()) {
            kafkaProducer.send(ressursRepository.findAllByProduced(false));
        }
    }
}
