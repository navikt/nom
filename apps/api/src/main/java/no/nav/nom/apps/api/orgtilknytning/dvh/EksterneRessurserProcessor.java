package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class EksterneRessurserProcessor {

    public static final String UNLEASH_LAG_EVENTS = "nom.persister_events_ved_last_orgtilknytninger_for_eksterne";
    private final Unleash unleash;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    public EksterneRessurserProcessor(
        Unleash unleash, RemedyEnhetMapperService remedyEnhetMapperService, OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService,
        OrgtilknytningCommandService orgtilknytningCommandService
    ) {
        this.unleash = unleash;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
    }

    public void process(DvhEksternRessurs eksternRessurs) {
        var eksisterendeOrgtilknytninger = orgtilknytningService.getByNavIdent(eksternRessurs.navident(), true);

        log.debug("Prosesserer ekstern ressurs {}\n{}", eksternRessurs.navident(), eksternRessurs.payload());
        if (!eksternRessurs.remedyAktiv()) {
            var aktiveOrgtilkntyninger = eksisterendeOrgtilknytninger.stream().filter( o -> o.gyldigTom().isEmpty()).toList();
            if (aktiveOrgtilkntyninger.isEmpty()) {
                log.debug("Ekstern ressurs {} er ikke aktiv - skipping", eksternRessurs.navident());
                return;
            } else {
                log.debug("Ekstern ressurs {} er ikke aktiv - men har orgtilknytninger som må avsluttes", eksternRessurs.navident());
                var meta = new InputMetadata("nom-api BatchJob", "Datavarehus", eksternRessurs, new Origination("nom-api", "nom-api"));
                aktiveOrgtilkntyninger.forEach( o ->
                    avsluttOrgtilknytning(o, eksternRessurs.remedySlutt(), meta));
            }
        }

        var orgenhet = getOrgenhetForEkstern(eksternRessurs);

        if (orgenhet == null) {
            String msg = String.format(
                "Ekstern ressurs %s - Fant ikke orgenhet for remedyEnhet %s eller agressoId/Nivå %s/%s. Kan ikke behandles.",
                eksternRessurs.navident(),
                eksternRessurs.remedyEnhetId(),
                eksternRessurs.orgenhetPaaRessurs(),
                eksternRessurs.orgenhetPaaRessursNivaa()
            );
            log.error(msg);
            return;
        }

        rapporterEvtDiffPaaRemedyenhet(eksternRessurs, orgenhet);


        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", eksternRessurs, new Origination("nom-api", "nom-api"));

        if (eksisterendeOrgtilknytninger.isEmpty()) {
            log.debug("Ekstern ressurs {} har ingen orgtilknytninger. Oppretter orgtilknytning med remedy_start som gyldigFom", eksternRessurs.navident());
            opprettOrgtilknytning(eksternRessurs, orgenhet, eksternRessurs.remedyStart(), metadata);
        } else {
            // Eneste endring vi forventer fra DVH er endring av remedy_slutt og/eller enhet
            // Vi trenger kun å sammenligne dette med siste orgtilknytningsperiode for identen.
            // Denne kan være med start i dag (var tidligere fremtidig, eller allerede endret orgtilknytning for den eksterne i dag)
            // Fremtidige orgtilknytninger er kun for eksterne som ikke har startet enda.
            var sisteEksisterendeOrgtilknytning = eksisterendeOrgtilknytninger.stream()
                .max(Comparator.comparing(OrgtilknytningState::gyldigFom))
                .get();

            if (harSammeSluttdato(eksternRessurs, sisteEksisterendeOrgtilknytning) && erSammeEnhet(sisteEksisterendeOrgtilknytning, orgenhet)) {
                log.debug("Ekstern ressurs {} har eksisterende orgtilknytning som ikke trenger endring", eksternRessurs.navident());
            } else {
                if (erSammeEnhet(sisteEksisterendeOrgtilknytning, orgenhet)) {
                    // Sluttdato er endret, endre sluttdato på eksisterende orgtilknytning - ingen flere endringer påkrevd.
                    endreSluttdatoPaaEksisterendeOrgtilknytning(sisteEksisterendeOrgtilknytning, eksternRessurs.remedySlutt(), metadata);
                } else {
                    // Hvis startdato på eksisterende er før dagens dato, avslutt eksisterende med sluttdato i går og opprett ny med startdato i dag
                    if (sisteEksisterendeOrgtilknytning.gyldigFom().isBefore(LocalDate.now())) {
                        avsluttOrgtilknytning(sisteEksisterendeOrgtilknytning, LocalDate.now().minusDays(1), metadata);
                        opprettOrgtilknytning(eksternRessurs, orgenhet, LocalDate.now(), metadata);
                    } else {
                        // Orgenhet er endret
                        // Fjern den eksisterende, opprett ny med samme gyldigFom
                        fjernEksisterendeOrgtilknytning(sisteEksisterendeOrgtilknytning, metadata);
                        opprettOrgtilknytning(eksternRessurs, orgenhet, sisteEksisterendeOrgtilknytning.gyldigFom(), metadata);
                    }
                }
            }
        }
    }

    private static boolean erSammeEnhet(OrgtilknytningState sisteEksisterendeOrgtilknytning, IOrgenhet orgenhet) {
        return sisteEksisterendeOrgtilknytning.orgEnhetId().equals(orgenhet.getId());
    }

    private static boolean harSammeSluttdato(DvhEksternRessurs eksternRessurs, OrgtilknytningState sisteEksisterendeOrgtilknytning) {
        return Objects.equals(sisteEksisterendeOrgtilknytning.gyldigTom().orElse(null), eksternRessurs.remedySlutt());
    }

    private void rapporterEvtDiffPaaRemedyenhet(DvhEksternRessurs eksternRessurs, IOrgenhet orgenhet) {
        var remedyEnhetForMappetOrgenhet = remedyEnhetMapperService.findByNomId(orgenhet.getId()).map(RemedyEnhet::remedyEnhetId).orElse(null);
        if (!Objects.equals(remedyEnhetForMappetOrgenhet, eksternRessurs.remedyEnhetId())) {
            String warn = String.format(
                "Ekstern ressurs %s er i DVH angitt remedyEnhet %s og agressoId/Nivå %s/%s er i NOM matchet til i orgenhet %s som mapper til en annen remedy enhet %s.",
                eksternRessurs.navident(),
                eksternRessurs.remedyEnhetId(),
                eksternRessurs.orgenhetPaaRessurs(),
                eksternRessurs.orgenhetPaaRessursNivaa(),
                orgenhet.getId(),
                remedyEnhetForMappetOrgenhet != null ? remedyEnhetForMappetOrgenhet : "'ingen remedyenhet mappet for NOM enhet'"
            );
            log.warn(warn);
        }
    }

    // Finner orgenhet for ekstern ressurs. orgenhetPaaRessurs har presedens over remedyEnhetId
    private IOrgenhet getOrgenhetForEkstern(DvhEksternRessurs eksternRessurs) {
        var navident = eksternRessurs.navident();
        var remedyEnhet = eksternRessurs.remedyEnhetId();
        var orgenhetPaaRessurs = eksternRessurs.orgenhetPaaRessurs();
        var orgenhetPaaRessursNivaa = eksternRessurs.orgenhetPaaRessursNivaa();

        Optional<IOrgenhet> orgenhetByRemedyEnhet = remedyEnhetMapperService
            .findByRemedyEnhetId(remedyEnhet)
            .map(
                orgenhet -> {
                    log.debug("For navident {} - Fant orgenhet {} for remedyEnhet {}", navident, orgenhet.nomId(), remedyEnhet);
                    return orgenhetService.getById(orgenhet.nomId());
                }
            ).orElseGet(
                () -> {
                    log.info("For navident {} - Fant ikke orgenhet for remedyEnhet {}", navident, remedyEnhet);
                    return Optional.empty();
                }
            );

        if (orgenhetPaaRessurs.startsWith("-")) {
            log.info(
                "For navident {} - ORGENHET_PAA_RESSURS i DVH er ikke angitt. DVH Kode: {}. Faller tilbake på å bruke remedyEnhet {}.",
                navident,
                orgenhetPaaRessurs,
                remedyEnhet
            );
            return orgenhetByRemedyEnhet.orElse(null);
        } else {
            var nomOrgenheterByOrgenhetPaaRessurs = orgenhetService.getByAgressoIdForEnheterMedOrganisering(orgenhetPaaRessurs)
                .stream()
                .filter(o -> Objects.equals(o.getOrgNiv(), orgenhetPaaRessursNivaa))
                .toList();
            if (nomOrgenheterByOrgenhetPaaRessurs.size() == 1) {
                log.info(
                    "For navident {} - Fant orgenhet {} for ORGENHET_PAA_RESSURS i DVH. DVH agressoId/Nivå: {}/{}.",
                    navident,
                    nomOrgenheterByOrgenhetPaaRessurs.get(0).getId(),
                    orgenhetPaaRessurs,
                    remedyEnhet
                );
                return nomOrgenheterByOrgenhetPaaRessurs.get(0);
            } else if (nomOrgenheterByOrgenhetPaaRessurs.isEmpty()) {
                log.info(
                    "For navident {} - Fant ikke orgenhet for agressoId/Nivå: {}/{}. Faller tilbake på å bruke remedyEnhet {}.",
                    navident,
                    orgenhetPaaRessurs,
                    orgenhetPaaRessursNivaa,
                    remedyEnhet
                );
            } else {
                log.info(
                    "For navident {} - Fant flere orgenheter for agressoId/Nivå: {}/{}. Faller tilbake på å bruke remedyEnhet {}.",
                    navident,
                    orgenhetPaaRessurs,
                    orgenhetPaaRessursNivaa,
                    remedyEnhet
                );
            }
            return orgenhetByRemedyEnhet.orElse(null);
        }
    }

    private void opprettOrgtilknytning(DvhEksternRessurs eksternRessurs, IOrgenhet orgenhet, LocalDate gyldigFom, InputMetadata metadata) {
        var opprettOrgtilknytning = new OpprettOrgtilknytning(
            eksternRessurs.navident(),
            orgenhet.getId(),
            gyldigFom,
            metadata,
            null
        );

        log.debug("Lager OpprettOrgtilknytning event: {}", opprettOrgtilknytning);

        String fid = null;
        if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
            fid = orgtilknytningCommandService.opprettOrgtilknytning(opprettOrgtilknytning);
        }

        if (eksternRessurs.remedySlutt() != null) {
            var avvikleOrgtilknytning = new AvvikleOrgtilknytning(
                eksternRessurs.navident(),
                orgenhet.getId(),
                eksternRessurs.remedySlutt(),
                metadata,
                null
            );

            log.debug("Lager AvvikleOrgtilknytning event: {}", avvikleOrgtilknytning);

            if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
                orgtilknytningCommandService.avvikleOrgtilknytning(fid, avvikleOrgtilknytning);
            }
        }

        var endreDagligOppfolgingOrgtilknytning = new EndreDagligOppfolgingOrgtilknytning(
            eksternRessurs.navident(),
            orgenhet.getId(),
            gyldigFom,
            Optional.ofNullable(eksternRessurs.remedySlutt()),
            true,
            metadata,
            null
        );

        log.debug("Lager EndreDagligOppfolgingOrgtilknytning event: {}", endreDagligOppfolgingOrgtilknytning);

        if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
            orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(fid, endreDagligOppfolgingOrgtilknytning);
        }
    }


    private void endreSluttdatoPaaEksisterendeOrgtilknytning(OrgtilknytningState orgtilknytning, LocalDate gyldigTom, InputMetadata metadata) {
        var endreOrgtilknytningDato = new EndreOrgtilknytningDato(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            orgtilknytning.gyldigFom(),
            Optional.ofNullable(gyldigTom),
            metadata,
            null
        );

        log.debug("Lager EndreOrgtilknytningDato event for fid {}:\n {}", orgtilknytning.fid(), endreOrgtilknytningDato);

        if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
            orgtilknytningCommandService.endreOrgtilknytningDato(orgtilknytning.fid(), endreOrgtilknytningDato);
        }
    }

    private void avsluttOrgtilknytning(OrgtilknytningState orgtilknytning, LocalDate gyldigTom, InputMetadata metadata) {
        var avvikleOrgtilknytning = new AvvikleOrgtilknytning(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            gyldigTom,
            metadata,
            null
        );

        log.debug("Lager AvvikleOrgtilknytning event for fid {}:\n {}", orgtilknytning.fid(), avvikleOrgtilknytning);

        if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
            orgtilknytningCommandService.avvikleOrgtilknytning(orgtilknytning.fid(), avvikleOrgtilknytning);
        }
    }

    private void fjernEksisterendeOrgtilknytning(OrgtilknytningState orgtilknytning, InputMetadata metadata) {
        var fjernOrgtilknytning = new FjernOrgtilknytning(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            orgtilknytning.gyldigFom(),
            orgtilknytning.gyldigTom(),
            metadata,
            null
        );

        log.debug("Lager FjernOrgtilknytning event for fid {}:\n {}", orgtilknytning.fid(), fjernOrgtilknytning);

        if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
            orgtilknytningCommandService.fjernOrgtilknytning(orgtilknytning.fid(), fjernOrgtilknytning);
        }
    }
}
