package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
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
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    public EksterneRessurserProcessor(
        Unleash unleash, RemedyEnhetMapperService remedyEnhetMapperService, OrgtilknytningStateService orgtilknytningStateService,
        OrgenhetService orgenhetService,
        OrgtilknytningCommandService orgtilknytningCommandService
    ) {
        this.unleash = unleash;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
    }

    public void process(DvhEksternRessurs eksternRessurs) {
        log.debug("Prosesserer ekstern ressurs {}\n{}", eksternRessurs.navident(), eksternRessurs.payload());
        if (!eksternRessurs.remedyAktiv()) {
            log.debug("Ekstern ressurs {} er ikke aktiv - skipping", eksternRessurs.navident());
            return;
        }

        var orgenhet = getOrgenhetForEkstern(eksternRessurs);

        if (orgenhet == null) {
            log.error("Ekstern ressurs {} - Fant ikke orgenhet for remedyEnhet {} eller agressoId/Nivå {}/{}. Kan ikke behandles.", eksternRessurs.navident(), eksternRessurs.remedyEnhetId(), eksternRessurs.orgenhetPaaRessurs(), eksternRessurs.orgenhetPaaRessursNivaa());
            return;
        }

        rapporterEvtDiffpaaRemedyenhet(eksternRessurs, orgenhet);

        InputMetadata metadata = new InputMetadata("nom-api BatchJob", "Datavarehus", eksternRessurs, new Origination("nom-api", "nom-api"));

        var eksisterendeOrgtilknytninger = orgtilknytningStateService.getOrgtilknytningerByNavIdent(eksternRessurs.navident());

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
                        fjernEksisterendeOrgtilknytning(sisteEksisterendeOrgtilknytning, orgenhet, metadata);
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

    private void rapporterEvtDiffpaaRemedyenhet(DvhEksternRessurs eksternRessurs, IOrgenhet orgenhet) {
        var remedyEnhetForMappetOrgenhet = remedyEnhetMapperService.findByNomId(orgenhet.getId()).map(RemedyEnhet::nomId).orElse(null);
        if (!Objects.equals(remedyEnhetForMappetOrgenhet, eksternRessurs.remedyEnhetId())) {
            log.warn("Ekstern ressurs {} med angitt remedyEnhet {} og agressoId/Nivå {}/{} er matchet til orgenhet {} i NOM som mapping til en annen remedy enhet {}.",
                eksternRessurs.navident(), eksternRessurs.remedyEnhetId(), eksternRessurs.orgenhetPaaRessurs(), eksternRessurs.orgenhetPaaRessursNivaa(),
                orgenhet.getId(),
                remedyEnhetForMappetOrgenhet != null ? remedyEnhetForMappetOrgenhet : "'ingen remedyenhet mappet for NOM enhet'"
            );
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
                    log.error("For navident {} - Fant ikke orgenhet for remedyEnhet {}", navident, remedyEnhet);
                    return Optional.empty();
                }
            );

        if (orgenhetPaaRessurs.startsWith("-")) {
            log.debug("For navident {} - ORGENHET_PAA_RESSURS i DVH er ikke angitt. DVH Kode: {}. Faller tilbake på å bruke remedyEnhet {}.", navident, orgenhetPaaRessurs, remedyEnhet);
            return orgenhetByRemedyEnhet.orElse(null);
        } else {
            var nomOrgenheterByOrgenhetPaaRessurs = orgenhetService.getByAgressoIdForEnheterMedOrganisering(orgenhetPaaRessurs).stream().filter( o -> Objects.equals(o.getOrgNiv(), orgenhetPaaRessursNivaa)).toList();
            if (nomOrgenheterByOrgenhetPaaRessurs.size() == 1) {
                log.debug("For navident {} - Fant orgenhet {} for ORGENHET_PAA_RESSURS i DVH. DVH Kode: {}. Faller tilbake på å bruke remedyEnhet {}.", navident, nomOrgenheterByOrgenhetPaaRessurs.get(0).getId(), orgenhetPaaRessurs, remedyEnhet);
                return nomOrgenheterByOrgenhetPaaRessurs.get(0);
            } else if (nomOrgenheterByOrgenhetPaaRessurs.isEmpty()) {
                log.error("For navident {} - Fant ikke orgenhet for agressoId/Nivå: {}/{}. Faller tilbake på å bruke remedyEnhet {}.", navident, orgenhetPaaRessurs, orgenhetPaaRessursNivaa, remedyEnhet);
            } else {
                log.error("For navident {} - Fant flere orgenheter for agressoId/Nivå: {}/{}. Faller tilbake på å bruke remedyEnhet {}.", navident, orgenhetPaaRessurs, orgenhetPaaRessursNivaa, remedyEnhet);
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
        var korrigerOrgtilknytningDato = new KorrigerOrgtilknytningDato(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            orgtilknytning.gyldigFom(),
            Optional.ofNullable(gyldigTom),
            metadata,
            null
        );

        log.debug("Lager KorrigerOrgtilknytningDato event for fid {}:\n {}", orgtilknytning.fid(), korrigerOrgtilknytningDato);

        if (unleash.isEnabled(UNLEASH_LAG_EVENTS)) {
            orgtilknytningCommandService.korrigerOrgtilknytningDato(orgtilknytning.fid(), korrigerOrgtilknytningDato);
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

    private void fjernEksisterendeOrgtilknytning(OrgtilknytningState orgtilknytning, IOrgenhet orgenhet, InputMetadata metadata) {
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
