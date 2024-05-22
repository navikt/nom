package no.nav.nom.apps.api.ressurs.implementations.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Slf4j
@Service
public class RessursHendelseForEventsourceService {
    private final RessursStateRepository ressursStateRepository;
    private final RessursCommandService ressursCommandService;

    private final NomMastered nomMastered;
    private final Unleash unleash;

    public RessursHendelseForEventsourceService(
        RessursStateRepository ressursStateRepository,
        RessursCommandService ressursCommandService, NomMastered nomMastered, Unleash unleash
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursCommandService = ressursCommandService;
        this.nomMastered = nomMastered;
        this.unleash = unleash;
    }

    public void handterRessursIderFraDvh(List<DvhRessurs> dvhRessurs) {
        log.info("Ressurser to process " + dvhRessurs.size());
        log.info("Sender meldinger ut på kafka");
        log.info("State repository size " + ressursStateRepository.getAll().size());
        Map<DvhRessurs, CompletableFuture<SendResult<String, RessursEvent>>> opprettetRessuser = new ConcurrentHashMap<>();

        dvhRessurs.forEach(ressurs -> {
            val ressursByPersonident = ressursStateRepository.findByPersonident(ressurs.getPersonIdent());
            ressursByPersonident.ifPresentOrElse(
                (r) -> createEndreHendelse(r, ressurs),
                () -> handterNyPersonident(ressurs, opprettetRessuser)
            );
        });

        opprettetRessuser.entrySet().stream().filter(entry -> entry.getKey().getSluttdato() != null).map(entry -> {
                try {
                    final var event = entry.getValue().get(5, TimeUnit.SECONDS).getProducerRecord().value();

                    return Map.entry(entry.getKey(), event);
                } catch (Exception e) {
                    log.error("Failed to send Opprett and sluttet event, will retry on next scheduled run", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(entry -> ressursSluttet(entry.getValue().getFid(), entry.getKey()));
    }

    private void handterNyPersonident(DvhRessurs ressurs, Map<DvhRessurs, CompletableFuture<SendResult<String, RessursEvent>>> opprettetRessuser) {
        // Håndter endre personident hvis det kun er endring av personident.
        // Altså - hvis det finnes Ressurs med samme navident i state, så anta at dette kun er endring av personident.

        final var ressursByNavident = ressursStateRepository.findByNavident(ressurs.getNavIdent());
        ressursByNavident.ifPresentOrElse((r) -> {
            if (!r.personident().equals(ressurs.getPersonIdent())) {
                if (skipDueToNomMastered(ressurs.getNavIdent())) {
                    return;
                }

                log.info(SECURE_LOGS, "Ressurs {} har fått ny personident {}", r.fid(), ressurs.getPersonIdent());
                ressursEndretPersonident(r.fid(), ressurs);
                // og denne kan ha andre endringer så sjekk dette i tillegg
                createEndreHendelse(r, ressurs);
            }
        }, () -> {
            if (skipDueToNomMastered(ressurs.getNavIdent())) {
                return;
            }

            log.info(SECURE_LOGS, "Ny personident {} fra DVH, kjenner ikke identen så oppretter ny", ressurs.getPersonIdent());
            final var taskEvent = opprettRessurs(ressurs);
            opprettetRessuser.put(taskEvent.getKey(), taskEvent.getValue());
        });
    }

    private Map.Entry<DvhRessurs, CompletableFuture<SendResult<String, RessursEvent>>> opprettRessurs(DvhRessurs ressurs) {
        if (skipDueToNomMastered(ressurs.getNavIdent())) {
            return null;
        }
        return Map.entry(ressurs, ressursCommandService.createRessursEvent(new OpprettRessursCommand(
            ressurs.getPersonIdent(),
            ressurs.getNavIdent(),
            Optional.empty(),
            DvhRessurs.toSektor(ressurs.getRemedyAnsattType()),
            toVisningsnavn(ressurs),
            null,
            createInputMetadata(ressurs.getRawData()),
            null,
            null,
            null,
            ressurs.getFysiskBrukersted()
        )));
    }

    private void createEndreHendelse(RessursState ressursState, DvhRessurs dvhRessurs) {
        if (skipDueToNomMastered(dvhRessurs.getNavIdent())) {
            return;
        }

        ressursState.sluttdato().ifPresentOrElse((stateSluttdato) -> {
            if (!stateSluttdato.equals(dvhRessurs.getSluttdato())) {
                ressursSluttet(ressursState.fid(), dvhRessurs);
            }
        }, () -> {
            if (dvhRessurs.getSluttdato() != null) {
                ressursSluttet(ressursState.fid(), dvhRessurs);
            }
        });

        ressursState.startdato().ifPresentOrElse((stateStartdato) -> {
            if (!stateStartdato.equals(dvhRessurs.getStartDato())) {
                ressursStartet(ressursState.fid(), dvhRessurs);
            }
        }, () -> {
            if (dvhRessurs.getStartDato() != null) {
                ressursStartet(ressursState.fid(), dvhRessurs);
            }
        });

        final var currentVisningsnavn = toVisningsnavn(dvhRessurs);

        if (!Objects.equals(ressursState.visningsnavn().getFormattedVisningsnavn(), currentVisningsnavn.getFormattedVisningsnavn())) {
            ressursEndretPersonalia(ressursState.fid(), dvhRessurs);
        }

        final var currentSektor = dvhRessurs.getRemedyAnsattType();

        if (currentSektor != null && !DvhRessurs.toSektor(currentSektor).equals(ressursState.sektor())) {
            ressursEndretSektor(ressursState.fid(), dvhRessurs);
        }

        ressursState.primaryFysiskBrukersted().ifPresentOrElse((s) -> {
            if (!s.equals(dvhRessurs.getFysiskBrukersted())) {
                ressursEndretFysiskBrukersted(ressursState.fid(), dvhRessurs);
            }
        }, () -> {
            if (dvhRessurs.getFysiskBrukersted() != null) {
                ressursEndretFysiskBrukersted(ressursState.fid(), dvhRessurs);
            }
        });
    }

    private void ressursSluttet(String fid, DvhRessurs dvhRessurs) {
        if (skipDueToNomMastered(dvhRessurs.getNavIdent())) {
            return;
        }

        ressursCommandService.avsluttRessursEvent(new OpphorRessursCommand(fid, dvhRessurs.getSluttdato()), createInputMetadata(dvhRessurs.getRawData()));
    }

    private void ressursStartet(String fid, DvhRessurs dvhRessurs) {
        if (skipDueToNomMastered(dvhRessurs.getNavIdent())) {
            return;
        }

        ressursCommandService.endreStartRessursEvent(new OppdaterStartdatoCommand(fid, dvhRessurs.getStartDato()), createInputMetadata(dvhRessurs.getRawData()));
    }

    private boolean skipDueToNomMastered(String navident) {
        if (unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS) && nomMastered.isMastered(navident)) {
            log.info("Ressurs {} er mastret i NOM så vil ikke behandle endring fra DVH", navident);
            return true;
        }
        return false;
    }

    private void ressursEndretPersonalia(String fid, DvhRessurs dvhRessurs) {
        if (skipDueToNomMastered(dvhRessurs.getNavIdent())) {
            return;
        }

        ressursCommandService.oppdaterPersonaliaEvent(
            new OppdaterPersonaliaCommand(fid, toVisningsnavn(dvhRessurs)),
            createInputMetadata(dvhRessurs.getRawData())
        );
    }

    private void ressursEndretSektor(String fid, DvhRessurs dvhRessurs) {
        if (skipDueToNomMastered(dvhRessurs.getNavIdent())) {
            return;
        }

        ressursCommandService.oppdaterSektorEvent(
            new OppdaterSektorCommand(fid, DvhRessurs.toSektor(dvhRessurs.getRemedyAnsattType())),
            createInputMetadata(dvhRessurs.getRawData())
        );
    }

    private void ressursEndretPersonident(String fid, DvhRessurs dvhRessurs) {
        if (skipDueToNomMastered(dvhRessurs.getNavIdent())) {
            return;
        }

        ressursCommandService.oppdaterPersonidentCommand(
            new OppdaterPersonidentCommand(fid, dvhRessurs.getPersonIdent()),
            createInputMetadata(dvhRessurs.getRawData())
        );
    }

    private void ressursEndretFysiskBrukersted(String fid, DvhRessurs dvhRessurs) {
        ressursCommandService.oppdaterFysiskeBrukerstederCommand(
            OppdaterFysiskeBrukerstederCommand.builder()
                .fid(fid)
                .primaryFysiskBrukersted(dvhRessurs.getFysiskBrukersted())
                .fysiskeBrukersteder(dvhRessurs.getFysiskBrukersted() != null ? Set.of(dvhRessurs.getFysiskBrukersted()) : Collections.emptySet())
                .build(),
            createInputMetadata(dvhRessurs.getRawData())
        );
    }

    private InputMetadata createInputMetadata(Object rawData) {
        return new InputMetadata("nom-api BatchJob", "Datavarehus", rawData, new Origination("nom-api", "nom-api"));
    }

    private Visningsnavn toVisningsnavn(DvhRessurs dvhRessurs) {
        return new Visningsnavn(dvhRessurs.getFornavn(), Optional.empty(), dvhRessurs.getEtternavn());
    }
}
