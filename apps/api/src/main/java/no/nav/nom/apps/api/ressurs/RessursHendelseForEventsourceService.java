package no.nav.nom.apps.api.ressurs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursHendelseForEventsourceService {
    private final RessursStateService ressursStateService;
    private final RessursCommadService ressursCommadService;


    public void handterRessursIderFraDvh(List<DvhRessurs> dvhRessurs) {
        log.info("Ressurser to process "+dvhRessurs.size());
        log.info("Sender meldinger ut på kafka");
        log.info("State repository size "+ressursStateService.getAll().size());
        Map<DvhRessurs, ListenableFuture<SendResult<String, Event>>> opprettetRessuser = new ConcurrentHashMap<>();

        dvhRessurs.forEach(ressurs -> {
            val ressursByPersonident = ressursStateService.findByPersonident(ressurs.getPersonIdent());

            ressursByPersonident.ifPresentOrElse(
                    (r) -> createEndreHendelse(r, ressurs),
                    () -> {
                        final var taskEvent = opprettRessurs(ressurs);
                        opprettetRessuser.put(taskEvent.getKey(), taskEvent.getValue());
                    }
            );
        });


        opprettetRessuser.entrySet().stream()
                .filter(entry -> entry.getKey().getSluttdato() != null)
                .map(entry -> {
                    try {
                        final var event = entry.getValue().get(5, TimeUnit.SECONDS)
                                .getProducerRecord()
                                .value();

                        return Map.entry(entry.getKey(), event);
                    } catch (Exception e){
                        log.error("Failed to send Opprett and sluttet event, will retry on next scheduled run", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(entry -> {
                    ressursSluttet(entry.getValue().getFid(), entry.getKey());
                });
    }

    public Map.Entry<DvhRessurs, ListenableFuture<SendResult<String, Event>>> opprettRessurs(DvhRessurs ressurs){
        return Map.entry(
                ressurs,
                ressursCommadService.createRessursEvent(
                        new OpprettRessursCommand(
                                ressurs.getPersonIdent(),
                                ressurs.getNavIdent(),
                                Optional.empty(),
                                DvhRessurs.toSektor(ressurs.getRemedyAnsattType()),
                                new Visningsnavn(
                                        ressurs.getFornavn(),
                                        null,
                                        ressurs.getEtternavn()
                                ),
                                createInputMetadata(ressurs.getRawData())
                        )
                )
        );
    }

    public void createEndreHendelse(RessursState ressursState, DvhRessurs dvhRessurs) {
        ressursState.sluttdato().ifPresentOrElse(
                (s) -> {
                    if (!s.equals(dvhRessurs.getSluttdato())){
                        ressursSluttet(ressursState.fid(), dvhRessurs);
                    }
                },
                () -> {
                    if (dvhRessurs.getSluttdato() != null){
                        ressursSluttet(ressursState.fid(), dvhRessurs);
                    }
                }
        );
    }

    public void ressursSluttet(String fid, DvhRessurs dvhRessurs){
        ressursCommadService.avsluttRessursEvent(
                new OpphorRessursCommand(
                        fid,
                        dvhRessurs.getSluttdato()
                ),
                createInputMetadata(dvhRessurs.getRawData())
        );
    }

    public InputMetadata createInputMetadata(Object rawData){
        return new InputMetadata(
                "nom-api BatchJob",
                "Datavarehus",
                rawData,
                new Origination("nom-api", "nom-api")
        );
    }
}
