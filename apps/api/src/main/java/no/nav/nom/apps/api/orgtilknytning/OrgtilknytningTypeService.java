package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterTilknytningtypeCommand;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static java.util.Objects.nonNull;

@Service
public class OrgtilknytningTypeService {

    private final static Logger logger = LoggerFactory.getLogger(OrgtilknytningTypeService.class);

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;
    private final RessursCommandService ressursCommandService;

    public OrgtilknytningTypeService(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        RessursStateRepository ressursStateRepository,
        NomMastered nomMastered,
        RessursCommandService ressursCommandService
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
        this.ressursCommandService = ressursCommandService;
    }

    // Tilknytningstypene "Uforutsett behov" og "Vikar" skal ikke lenger benyttes i NOM.
    // Alle OTer som har disse tilknytningtypene skal heller få tilknytningstype "Midlertidig".
    // Dette gjelder også OTer som ikke lenger er aktive.
    public void replaceUforutsettbehovOgVikarOrgtilknytningTypeTilMidlertidig() {
        var alleOrgtilknytninger = orgtilknytningStateRepository.getAll()
            .stream()
            .filter(orgtilknytningState -> nonNull(orgtilknytningState.orgtilknytningType()))
            .filter(orgtilknytningState -> nomMastered.isMastered(orgtilknytningState.navident()))
            .toList();

        InputMetadata inputMetadata = InputMetadata.builder()
            .inputInterfaceName("nom-api")
            .inputInterfaceMethodName("replaceUforutsettbehovOgVikarOrgtilknytningTypeTilMidlertidig")
            .origination(new Origination("nom-api", "nom-api"))
            .build();

        //TODO: dry run first and to be removed later
        boolean dryRun = true;
        alleOrgtilknytninger.forEach(orgtilknytningState ->  ressursStateRepository.findByNavident(orgtilknytningState.navident()).ifPresent(ressursState -> {
            if(orgtilknytningState.orgtilknytningType().equals(Tilknytningtype.UFORUTSETT_BEHOV)  || orgtilknytningState.orgtilknytningType().equals(Tilknytningtype.VIKAR)) {
                OppdaterTilknytningtypeCommand oppdaterTilknytningtypeCommand = new OppdaterTilknytningtypeCommand(ressursState.fid(), Tilknytningtype.MIDLERTIDIG);
                if(dryRun) {
                    logger.info("Found Tilknytningstypene Uforutsett behov or Vikar, and will be replaced to Midlertidig. " + orgtilknytningState);
                } else {
                    ressursCommandService.oppdaterTilknytningtypeEvent(oppdaterTilknytningtypeCommand, inputMetadata);
                }
            }
        }));
    }

}
