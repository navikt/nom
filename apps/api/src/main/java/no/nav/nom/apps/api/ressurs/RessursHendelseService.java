package no.nav.nom.apps.api.ressurs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.eventsourcing.state.RessursStateService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursHendelseService {
    private final RessursStateService ressursStateService;
    private final RessursCommadService ressursCommadService;


    public void handterRessursIderFraDvh(List<DvhRessurs> dvhRessurs) {
        dvhRessurs.forEach(ressurs -> {
            val ressursByPersonident = ressursStateService.findByPersonident(ressurs.getPersonIdent());

            ressursByPersonident.ifPresentOrElse(
                    (r) -> createEndreHendelse(r, ressurs),
                    () -> opprettRessurs(ressurs)
            );
        });
    }

    public void opprettRessurs(DvhRessurs ressurs){
        ressursCommadService.createRessursEvent(
                new OpprettRessursCommand(
                        ressurs.getPersonIdent(),
                        ressurs.getNavIdent(),
                        DvhRessurs.toSektor(ressurs.getRemedyAnsattType()),
                        new RessursOpprettetEventV1.RessursRegisteredEventData.Visningsnavn(
                                ressurs.getFornavn(),
                                null,
                                ressurs.getEtternavn()
                        )
                ),
                createInputMetadata(ressurs.getRawData())
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

    public void ressursSluttet(UUID fid, DvhRessurs dvhRessurs){
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
                null,
                new Origination("nom-api", "nom-api")
        );
    }
}
