package no.nav.nom.apps.nom.ressurser.producer;

import lombok.AllArgsConstructor;
import no.nav.nom.apps.nom.ressurser.common.LeaderElection;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@AllArgsConstructor
public class KafkaScheduler {

    private final RessursRepository ressursRepository;
    private final KafkaProducer kafkaProducer;
    private final LeaderElection leaderElection;

    @Scheduled(fixedDelay = 5000)
    public void produce(){
        if(leaderElection.isLeader()) {
            kafkaProducer.send(ressursRepository.findAllByProduced(false));
        }
    }
}
