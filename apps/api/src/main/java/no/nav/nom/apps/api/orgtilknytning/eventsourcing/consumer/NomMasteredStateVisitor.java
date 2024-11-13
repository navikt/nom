package no.nav.nom.apps.api.orgtilknytning.eventsourcing.consumer;

import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.util.Objects.isNull;

@Component
public class NomMasteredStateVisitor {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;

    public NomMasteredStateVisitor(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningCommandService orgtilknytningCommandService,
        DvhStartupLoadMonitor dvhStartupLoadMonitor
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
    }

    @KafkaListener(
        id = "OrgtilknytningSektorOppdatering",
        containerFactory = "nomStateMasteredRessursKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.mastered-ressurs.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, LocalDateTime> records) {
        dvhStartupLoadMonitor.awaitInitialLoadComplete();
        records.forEach(record -> {
            if (!isNull(record.value())) {
                ressursStateRepository.findByNavident(record.key()).ifPresent(this::setSektorPaaEksisterendeOrgtilknytning);
            }
        });
    }

    private void setSektorPaaEksisterendeOrgtilknytning(RessursState ressursState) {
        var orgtilknytninger = orgtilknytningStateRepository.findByNavident(ressursState.navident()).stream().filter(this::isActiveOrFuture).toList();

        if (orgtilknytninger.isEmpty()) {
            return;
        }

        var ortilknytningMedSektor = orgtilknytninger.stream().filter(orgtilknytningState -> !isNull(orgtilknytningState.sektor())).toList();
        var metadata = new InputMetadata(
            "nom-api NomMasteredStateVisitor",
            "nom-api - Sektor auto periodisering av orgtilknytning",
            null,
            new Origination("nom-api", "NomMasteredStateVisitor")

        );
        if (ortilknytningMedSektor.isEmpty()) {
            orgtilknytninger.forEach(orgtilknytningState -> {
                if (isNull(orgtilknytningState.sektor())) {
                    orgtilknytningCommandService.endreOrgtilknytningSektor(
                        new EndreOrgtilknytningSektorCommand(
                            orgtilknytningState.fid(),
                            orgtilknytningState.navident(),
                            orgtilknytningState.orgEnhetId(),
                            ressursState.sektor(),
                            metadata,
                            null
                        ));
                }
            });
        } else {
            var orgtilknytningDOL = ortilknytningMedSektor.stream()
                .filter(orgtilknytningState -> orgtilknytningState.gyldigFom().isAfter(LocalDate.now()))
                .filter(OrgtilknytningState::erDagligOppfolging)
                .findFirst();

            orgtilknytninger.forEach(orgtilknytningState -> {
                if (isNull(orgtilknytningState.sektor())) {
                    orgtilknytningCommandService.endreOrgtilknytningSektor(
                        new EndreOrgtilknytningSektorCommand(
                            orgtilknytningState.fid(),
                            orgtilknytningState.navident(),
                            orgtilknytningState.orgEnhetId(),
                            orgtilknytningDOL.isPresent() ? orgtilknytningDOL.get().sektor() : ortilknytningMedSektor.getFirst().sektor(),
                            metadata,
                            null
                        ));
                }
            });
        }
    }

    private boolean isActiveOrFuture(OrgtilknytningState orgtilknytningState) {
        return isActive(orgtilknytningState) || isFuture(orgtilknytningState);
    }

    private boolean isFuture(OrgtilknytningState orgtilknytningState) {
        return orgtilknytningState.gyldigFom().isAfter(LocalDate.now());
    }

    private boolean isActive(OrgtilknytningState orgtilknytningState) {
        return !orgtilknytningState.gyldigFom().isAfter(LocalDate.now())
            && (orgtilknytningState.gyldigTom().isEmpty() || !orgtilknytningState.gyldigTom().get().isBefore(LocalDate.now()));

    }
}
