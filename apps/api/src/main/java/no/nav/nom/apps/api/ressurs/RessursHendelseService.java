package no.nav.nom.apps.api.ressurs;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.eventsourcing.state.KafkaStateRessursService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursHendelseService {
    private final KafkaStateRessursService kafkaStateRessursService;
    private final RessursCommadService ressursCommadService;


    public void handterRessursIderFraDvh(List<DvhRessurs> dvhRessurs) {
        dvhRessurs.forEach(ressurs -> {
            val ressursByPersonident = kafkaStateRessursService.findByPersonident(ressurs.getPersonIdent());

            ressursByPersonident.ifPresentOrElse(
                    (r) -> createEndreHendelse(r, ressurs),
                    () -> ressursCommadService.createRessursEvent(
                            new OpprettRessursCommand(
                                    ressurs.getPersonIdent(),
                                    ressurs.getNavIdent(),
                                    toSektor(ressurs.getRemedyAnsattType()),
                                    new RessursOpprettetEventV1.RessursRegisteredEventData.Visningsnavn(
                                            ressurs.getFornavn(),
                                            null,
                                            ressurs.getEtternavn()
                                    )
                            ),
                            createInputMetadata(ressurs.getRawData())
                    )
            );
        });
    }

    @SneakyThrows
    public void createEndreHendelse(RessursState ressursState, DvhRessurs dvhRessurs) {
        /*Map<String, Object> changes = new HashMap<>();
        if(!ressursEventAggregate.getPersoident().equals(dvhRessurs.getPersonIdent())){
            changes.put(RessursEvent.PERSONIDENT_KEY, dvhRessurs.getPersonIdent());
        }
        if(
                (!ressursEventAggregate.getVisningsnavn().fornavn().equals(dvhRessurs.getFornavn()) &&
                !(ressursEventAggregate.getVisningsnavn().fornavn()+" "+ressursEventAggregate.getVisningsnavn().mellomnavn()).equals(dvhRessurs.getFornavn())) ||
                !ressursEventAggregate.getVisningsnavn().etternavn().equals(dvhRessurs.getEtternavn())

        ){
            changes.put(RessursEvent.VISNINGSNAVN_KEY, new RessursEvent.Visningsnavn(dvhRessurs.getFornavn(), null, dvhRessurs.getEtternavn()));
        }
        eventProducer.sendMessage(
                new UpdateRessursCommand(ressursEventAggregate.getFid(),
                        changes,
                        new NomBatchJobInputMetadata(inputInterfaceName, dvhRessurs.getRawData())
                ).toEvent()
        );*/
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

    public RessursOpprettetEventV1.RessursRegisteredEventData.Sektor toSektor(String remedyAnsattType){
        if (remedyAnsattType.equals("NAV Statlig")){
            return RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.NAV_STATLIG;
        } else if (remedyAnsattType.equals("NAV Kommunalt")){
            return RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.NAV_KOMMUNAL;
        } else {
            return RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.EKSTERN;
        }
    }
}
