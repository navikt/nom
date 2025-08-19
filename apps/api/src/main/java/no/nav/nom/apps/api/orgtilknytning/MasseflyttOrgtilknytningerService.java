package no.nav.nom.apps.api.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.api.email.OrgtilknytningEndretEmailService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;

/*
 * REGLER:<
 *
 * 1. A slutter før flyttedato<
 * 	A - urørt	- meldes som ikke flyttet hvis den legges til for flytting
 *
 * 				|
 * 		<---A-->
 *
 * 	Resultat:
 * 				|
 * 		<---A-->
 *
 *
 * 2. A begynner etter flyttedato
 * 	A - skal fjernes, og ny opprettes på destinasjon med opprinnelig A sin startdato og sluttdato
 *
 * 	  |
 * 		<---A-->
 *
 * 	Resultat:
 * 	  |
 * 		<--B--->
 *
 * 3. A begynner på flyttedato
 * 	A - skal fjernes, og ny opprettes på destinasjon med opprinnelig A sin startdato og sluttdato
 *
 * 	    |
 * 		<---A-->
 *
 * 	Resultat:
 * 	  	|
 * 		<--B--->
 *
 * 4. A slutter på flyttedato, B starter dagen etter
 * 	A - Skal avsluttes dagen før flyttedato, får ny med fra-til lik hverandre på flyttedato (1.dag gyldighet....) på destinasjon
 *
 *    		   |
 * 		<---A-->
 *
 * 	Resultat:
 *            |
 * 		<---A->
 * 			  >< B
 *
 * 5. A begynner dagen før flyttedato
 * 	A - skal avsluttes dagen før flyttedato - A sin startdato - start slutt blir lik. (1. dag gyldighet)
 * 	B - skal opprettes med start på flyttedato på destinasjon, Med opprinnelig A sin sluttdato
 *
 * 	     |
 * 		<---A-->
 *
 * 	Resultat:
 * 	  	 |
 * 		>< A
 * 		 <--B-->
 *
 * 6. Flyttdato i perioden til A
 * 	A - skal avsluttes dagen før flyttedato
 * 	B - skal opprettes med start på flyttedato på destinasjon, Med opprinnelig A sin sluttdato
 *
 * 			|
 * 		<---A-->
 *
 * 	Resultat
 * 			|
 * 		<-A>
 * 			<-B>
 */
@Slf4j
@Service
public class MasseflyttOrgtilknytningerService {
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final NomMastered nomMastered;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;

    public MasseflyttOrgtilknytningerService(OrgtilknytningStateRepository orgtilknytningStateRepository, OrgenhetService orgenhetService,
                                             OrgtilknytningCommandService orgtilknytningCommandService, NomMastered nomMastered,
                                             NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
                                             OrgtilknytningEndretEmailService orgtilknytningEndretEmailService
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.nomMastered = nomMastered;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.orgtilknytningEndretEmailService = orgtilknytningEndretEmailService;
    }

    public HashMap<OrgtilknytningState, String> flyttOrgtilknytninger(List<String> orgtilknytningFids, String nyOrgenhetId, boolean beholdTilganger, LocalDate gyldigFomFlyttedato, InputMetadata inputMetadata) {
        return flyttOrgtilknytninger(orgtilknytningFids, nyOrgenhetId, beholdTilganger, gyldigFomFlyttedato, inputMetadata, false);
    }

    public HashMap<OrgtilknytningState, String> flyttOrgtilknytninger(List<String> orgtilknytningFids, String nyOrgenhetId, boolean beholdTilganger, LocalDate gyldigFomFlyttedato, InputMetadata inputMetadata, boolean ventPaaState) {
        var flyttedato = gyldigFomFlyttedato == null ? LocalDate.now() : gyldigFomFlyttedato;

        log.info("Flytter orgtilknytninger {} til orgenhet {} fra dato {}. Sett 'masseflytt flagg' i Remedy {}", orgtilknytningFids, nyOrgenhetId, flyttedato, beholdTilganger);

        List<OrgtilknytningState> orgtilknytninger = orgtilknytningStateRepository.findByFidIn(orgtilknytningFids);
        IOrgenhet nyOrgenhet = orgenhetService.getById(nyOrgenhetId).orElseThrow(() -> new IllegalArgumentException("Angitt ny orgenhet id finnes ikke"));

        if (orgtilknytninger.size() > orgtilknytningFids.size()) {
            throw new IllegalArgumentException("Noen eller alle angitt orgtilknytning FIDs finnes ikke");
        }

        if (!nyOrgenhet.erAktivForDato(flyttedato)) {
            throw new IllegalArgumentException("Angitt ny orgenhet er ikke aktiv på flyttedato");
        }

        var orgtilknytningerIkkeFlyttet = new HashMap<OrgtilknytningState, String>();
        List<OrgtilknytningState> orgtilknytningerFlyttet = new ArrayList<>();

        orgtilknytninger.forEach(ot -> {

            if (!etter(ot.gyldigTom(), flyttedato)) {
                orgtilknytningerIkkeFlyttet.put(ot, "Orgtilknytning er ikke gyldig for flytting på angitt flyttedato");
                return;
            }
            if (etter(ot.gyldigTom(), nyOrgenhet.getGyldigTom().orElse(null))) {
                orgtilknytningerIkkeFlyttet.put(ot, "Orgtilknytning er gyldig etter ny orgenhet sin gyldighetsperiode");
                return;
            }

            if (likEllerEtter(ot.gyldigFom(), flyttedato)) {
                orgtilknytningCommandService.fjernOrgtilknytning(ot.fid(), new FjernOrgtilknytning(
                    ot.navident(),
                    ot.orgEnhetId(),
                    ot.gyldigFom(),
                    ot.gyldigTom(),
                    inputMetadata,
                    null
                ));

                // Må fjerne eksisterende OT og lage ny på destinasjon
                lagNyOrgtilknytning(
                    ot.fid(),
                    ot.navident(),
                    nyOrgenhetId,
                    beholdTilganger,
                    ot.gyldigFom(),
                    ot.gyldigTom(),
                    ot.sektor(),
                    ot.orgtilknytningType(),
                    ot.erDagligOppfolging(),
                    inputMetadata,
                    ventPaaState
                );
            } else {
                // Avslutt eksisterende OT på dagen før flyttedato, og lage ny på destinasjon fra flyttedato
                orgtilknytningCommandService.avvikleOrgtilknytning(ot.fid(), new AvvikleOrgtilknytning(
                    ot.navident(),
                    ot.orgEnhetId(),
                    flyttedato.minusDays(1),
                    inputMetadata,
                    null
                ));

                lagNyOrgtilknytning(
                    ot.fid(),
                    ot.navident(),
                    nyOrgenhetId,
                    beholdTilganger,
                    flyttedato,
                    ot.gyldigTom(),
                    ot.sektor(),
                    ot.orgtilknytningType(),
                    ot.erDagligOppfolging(),
                    inputMetadata,
                    ventPaaState
                );
            }

            nomMastered.markRessursAsMasteredInNom(ot.navident());

            orgtilknytningerFlyttet.add(ot);
        });

        orgtilknytningEndretEmailService.sendMasseflyttOTEmail(nyOrgenhetId, orgtilknytningerFlyttet, orgtilknytningerIkkeFlyttet.keySet());

        return orgtilknytningerIkkeFlyttet;
    }

    private void lagNyOrgtilknytning(
        String originalFid,
        String navident,
        String nyOrgenhetId,
        boolean beholdTilganger,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        Sektor sektor,
        Tilknytningtype tilknytningType,
        boolean isDOL,
        InputMetadata inputMetadata,
        boolean ventPaaState
    ) {
        var latch = setupStateOppdatertLytter(2 + (gyldigTom != null ? 1 : 0) + (tilknytningType != null ? 1 : 0), ventPaaState);

        var newFid = orgtilknytningCommandService.opprettOrgtilknytning(new OpprettOrgtilknytning(
            navident,
            nyOrgenhetId,
            sektor,
            gyldigFom,
            beholdTilganger ? originalFid : null,
            inputMetadata,
            null
        ));

        if (gyldigTom != null) {
            orgtilknytningCommandService.avvikleOrgtilknytning(
                newFid,
                new AvvikleOrgtilknytning(
                    navident,
                    nyOrgenhetId,
                    gyldigTom,
                    inputMetadata,
                    null
                )
            );
        }

        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(new EndreDagligOppfolgingOrgtilknytningCommand(
            newFid,
            navident,
            nyOrgenhetId,
            gyldigFom,
            gyldigTom,
            isDOL,
            inputMetadata,
            null
        ));

        if (tilknytningType != null) {
            orgtilknytningCommandService.endreTilknytningtype(new EndreTilknytningtypeCommand(
                newFid,
                tilknytningType,
                inputMetadata
            ));
        }

        ventPaaState(latch, ventPaaState);
    }

    private AbstractNomStateListener.Visitor<OrgtilknytningState> visitor;

    private CountDownLatch setupStateOppdatertLytter(int numOppdatertinger, boolean ventPaaState) {
        if (!ventPaaState) {
            return null;
        }
        CountDownLatch latch = new CountDownLatch(numOppdatertinger);
        visitor = new AbstractNomStateListener.Visitor<>() {
            @Override
            public String visitorName() {
                return "Masseflytt OT listener";
            }
            @Override
            public void visit(String key, OrgtilknytningState stateObject) {
                latch.countDown();
            }
        };
        nomStateOrgtilknytningListener.register(visitor);
        return latch;
    }

    private void ventPaaState(CountDownLatch latch, boolean vent) {
        if (!vent) {
            return;
        }
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                log.warn("Orgtilknytning state sent on topic is never read under masseflytt - fortsetter allikevel");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        nomStateOrgtilknytningListener.unregister(visitor);
    }
}
