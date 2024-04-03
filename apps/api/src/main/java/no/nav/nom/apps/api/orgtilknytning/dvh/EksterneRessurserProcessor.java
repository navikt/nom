package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.Tilknytningtype;
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

    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final NomMastered nomMastered;
    private final Unleash unleash;

    public EksterneRessurserProcessor(
            RemedyEnhetMapperService remedyEnhetMapperService, OrgtilknytningService orgtilknytningService,
            OrgenhetService orgenhetService, OrganiseringStateRepository organiseringStateRepository,
            OrgtilknytningCommandService orgtilknytningCommandService, NomMastered nomMastered, Unleash unleash
    ) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.nomMastered = nomMastered;
        this.unleash = unleash;
    }

    public void process(DvhEksternRessurs eksternRessurs) {
        var eksisterendeOrgtilknytninger = orgtilknytningService.getByNavIdent(eksternRessurs.navident());

        log.debug("Prosesserer ekstern ressurs {}\n{}", eksternRessurs.navident(), eksternRessurs.payload());
        if (!eksternRessurs.remedyAktiv()) {
            var aktiveOrgtilkntyninger = eksisterendeOrgtilknytninger.stream().filter( o -> o.gyldigTom().isEmpty()).toList();
            if (aktiveOrgtilkntyninger.isEmpty()) {
                log.debug("Ekstern ressurs {} er ikke aktiv - skipping", eksternRessurs.navident());
                return;
            } else {
                log.debug("Ekstern ressurs {} er ikke aktiv - men har orgtilknytninger som må avsluttes", eksternRessurs.navident());
                var meta = new InputMetadata("nom-api BatchJob", "Datavarehus", eksternRessurs, new Origination("nom-api", "nom-api"));

                // hvis aktiv orgtilknytning starter etter RemedySlutt, så fjern den i steden for å avslutte den
                aktiveOrgtilkntyninger.forEach( o -> {
                    if (eksternRessurs.remedySlutt() != null && o.gyldigFom().plusDays(1).isAfter(eksternRessurs.remedySlutt())) {
                        fjernEksisterendeOrgtilknytning(o, meta);
                    } else {
                        avsluttOrgtilknytning(o, eksternRessurs.remedySlutt(), meta);
                    }
                });
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
                // Oppdater evt tilknytningstypen - hvis denne er ikke er korrekt
                settTypeEksternSamarbeidspartnerHvisSkatt(sisteEksisterendeOrgtilknytning, metadata);
            } else {
                if (erSammeEnhet(sisteEksisterendeOrgtilknytning, orgenhet)) {
                    // Sluttdato er endret, endre sluttdato på eksisterende orgtilknytning - ingen flere endringer påkrevd.
                    // Hvis sluttdato er før ekstisterende startdato, fjern orgtilknytingen
                    if (eksternRessurs.remedySlutt() != null && sisteEksisterendeOrgtilknytning.gyldigFom().plusDays(1).isAfter(eksternRessurs.remedySlutt())) {
                        fjernEksisterendeOrgtilknytning(sisteEksisterendeOrgtilknytning, metadata);
                    } else {
                        endreSluttdatoPaaEksisterendeOrgtilknytning(sisteEksisterendeOrgtilknytning, eksternRessurs.remedySlutt(), metadata);
                    }
                } else {
                    if (eksternRessurs.remedySlutt() != null && LocalDate.now().isAfter(eksternRessurs.remedySlutt())) {
                        // Hvis sluttdato er før dagens dato, avslutt eksisterende
                        avsluttOrgtilknytning(sisteEksisterendeOrgtilknytning, eksternRessurs.remedySlutt(), metadata);
                    } else if (sisteEksisterendeOrgtilknytning.gyldigFom().isBefore(LocalDate.now())) {
                        // Hvis startdato på eksisterende er før dagens dato, avslutt eksisterende med sluttdato i går og opprett ny med startdato i dag
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

        IOrgenhet orgenhetByRemedyEnhet = remedyEnhetMapperService
            .findByRemedyEnhetId(remedyEnhet)
            .map(
                orgenhet -> {
                    log.debug("For navident {} - Fant orgenhet {} for remedyEnhet {}", navident, orgenhet.nomId(), remedyEnhet);
                    var orgenhetForRemedyenhet = orgenhetService.getById(orgenhet.nomId()).orElse(null);
                    orgenhetForRemedyenhet = hvisSkattPlasserNederst(orgenhetForRemedyenhet, navident, remedyEnhet);
                    return orgenhetForRemedyenhet;
                }
            ).orElseGet(
                () -> {
                    log.info("For navident {} - Fant ikke orgenhet for remedyEnhet {}", navident, remedyEnhet);
                    return null;
                }
            );

        if (orgenhetPaaRessurs.startsWith("-")) {
            log.info(
                "For navident {} - ORGENHET_PAA_RESSURS i DVH er ikke angitt. DVH Kode: {}. Faller tilbake på å bruke remedyEnhet {}.",
                navident,
                orgenhetPaaRessurs,
                remedyEnhet
            );
            return orgenhetByRemedyEnhet;
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
                var nomOrgenhetPaaRessurs = nomOrgenheterByOrgenhetPaaRessurs.get(0);
                nomOrgenhetPaaRessurs = hvisSkattPlasserNederst(nomOrgenhetPaaRessurs, navident, remedyEnhet);
                return nomOrgenhetPaaRessurs;
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
            return orgenhetByRemedyEnhet;
        }
    }

    private IOrgenhet hvisSkattPlasserNederst(IOrgenhet orgenhetForRemedyenhet, String navident, String remedyEnhet) {
        if (erSkatteetatenNiv0Enhet(orgenhetForRemedyenhet)) {
            // Skatt skal plasseres på nivået under
            var underliggende = orgenhetService.findAllUnderliggendeOrgenheter(orgenhetForRemedyenhet.getId());
            if (underliggende.isEmpty()) {
                log.warn("SKATT ({}) har ingen underenheter. Faller tilbake på å bruke orgenhet {}.", orgenhetForRemedyenhet.getId(), orgenhetForRemedyenhet.getId());
            } else if (underliggende.size() == 1) {
                orgenhetForRemedyenhet = orgenhetService.getById(underliggende.get(0)).orElse(null);
                log.debug("For navident {} - Fant underenhet av SKATT: orgenhet {} for remedyEnhet {}", navident, orgenhetForRemedyenhet.getId(), remedyEnhet);
            } else {
                log.warn("For navident {} - Fant flere underenheter for orgenhet SKATT ({}). Faller tilbake på å bruke orgenhet {}.",
                    navident, orgenhetForRemedyenhet.getId(), orgenhetForRemedyenhet.getId());
            }
        }
        return orgenhetForRemedyenhet;
    }

    private boolean erSkatteetatenNiv0Enhet(IOrgenhet orgenhetForRemedyenhet) {
        return orgenhetForRemedyenhet != null && orgenhetForRemedyenhet.getAgressoId().equals("SKATT");
    }

    private boolean erUnderSkatteetatenNiv0Enhet(IOrgenhet orgenhet) {
        var organisering = organiseringStateRepository.findActiveById(orgenhet.getId());
        if (organisering.isEmpty()) {
            return false;
        }
        return erSkatteetatenNiv0Enhet(orgenhet) || orgenhetService.getById(organisering.get().idOver()).map(this::erSkatteetatenNiv0Enhet).orElse(false);
    }

    private void opprettOrgtilknytning(DvhEksternRessurs eksternRessurs, IOrgenhet orgenhet, LocalDate gyldigFom, InputMetadata metadata) {
        if (unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS) && nomMastered.isMastered(eksternRessurs.navident())) {
            log.info("Navident {} er mastered. Vi lager ikke orgtilknytning for denne", eksternRessurs.navident());
            return;
        }

        var opprettOrgtilknytning = new OpprettOrgtilknytning(
            eksternRessurs.navident(),
            orgenhet.getId(),
            gyldigFom,
            metadata,
            null
        );

        log.debug("Lager OpprettOrgtilknytning event: {}", opprettOrgtilknytning);

        String fid = null;
        fid = orgtilknytningCommandService.opprettOrgtilknytning(opprettOrgtilknytning);

        if (eksternRessurs.remedySlutt() != null) {
            var avvikleOrgtilknytning = new AvvikleOrgtilknytning(
                eksternRessurs.navident(),
                orgenhet.getId(),
                eksternRessurs.remedySlutt(),
                metadata,
                null
            );

            log.debug("Lager AvvikleOrgtilknytning event: {}", avvikleOrgtilknytning);

            orgtilknytningCommandService.avvikleOrgtilknytning(fid, avvikleOrgtilknytning);
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

        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(fid, endreDagligOppfolgingOrgtilknytning);

        if (erUnderSkatteetatenNiv0Enhet(orgenhet)) {
            settTilknytningtype(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER, fid, eksternRessurs.navident(), metadata);
        }
    }


    private void endreSluttdatoPaaEksisterendeOrgtilknytning(OrgtilknytningState orgtilknytning, LocalDate gyldigTom, InputMetadata metadata) {
        if (unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS) && nomMastered.isMastered(orgtilknytning.navident())) {
            log.info("Navident {} er mastered. Vi endrer ikke sluttdato på orgtilknytning for denne", orgtilknytning.navident());
            return;
        }

        var endreOrgtilknytningDato = new EndreOrgtilknytningDato(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            orgtilknytning.gyldigFom(),
            Optional.ofNullable(gyldigTom),
            metadata,
            null
        );

        log.debug("Lager EndreOrgtilknytningDato event for fid {}:\n {}", orgtilknytning.fid(), endreOrgtilknytningDato);

        orgtilknytningCommandService.endreOrgtilknytningDato(orgtilknytning.fid(), endreOrgtilknytningDato);

        settTypeEksternSamarbeidspartnerHvisSkatt(orgtilknytning, metadata);
    }

    private void avsluttOrgtilknytning(OrgtilknytningState orgtilknytning, LocalDate gyldigTom, InputMetadata metadata) {
        if (unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS) && nomMastered.isMastered(orgtilknytning.navident())) {
            log.info("Navident {} er mastered. Vi avslutter ikke orgtilknytning for denne", orgtilknytning.navident());
            return;
        }

        var avvikleOrgtilknytning = new AvvikleOrgtilknytning(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            gyldigTom,
            metadata,
            null
        );

        log.debug("Lager AvvikleOrgtilknytning event for fid {}:\n {}", orgtilknytning.fid(), avvikleOrgtilknytning);

        orgtilknytningCommandService.avvikleOrgtilknytning(orgtilknytning.fid(), avvikleOrgtilknytning);
    }

    private void settTilknytningtype(Tilknytningtype type, String orgtilknytningFid, String navident, InputMetadata metadata) {
        if (unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS) && nomMastered.isMastered(navident)) {
            log.info("Navident {} er mastered. Vi avslutter ikke orgtilknytning for denne", navident);
            return;
        }

        var endreTilknytningsType =  new EndreTilknytningtypeCommand(
            orgtilknytningFid,
            Optional.of(type),
            metadata
        );

        log.debug("Endrer tilknytningstype til {} for navident {}.", type, navident);

        orgtilknytningCommandService.endreTilknytningtype(endreTilknytningsType);
    }


    private void fjernEksisterendeOrgtilknytning(OrgtilknytningState orgtilknytning, InputMetadata metadata) {
        if (unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS) && nomMastered.isMastered(orgtilknytning.navident())) {
            log.info("Navident {} er mastered. Vi fjerner ikke orgtilknytning for denne", orgtilknytning.navident());
            return;
        }

        var fjernOrgtilknytning = new FjernOrgtilknytning(
            orgtilknytning.navident(),
            orgtilknytning.orgEnhetId(),
            orgtilknytning.gyldigFom(),
            orgtilknytning.gyldigTom(),
            metadata,
            null
        );

        log.debug("Lager FjernOrgtilknytning event for fid {}:\n {}", orgtilknytning.fid(), fjernOrgtilknytning);

        orgtilknytningCommandService.fjernOrgtilknytning(orgtilknytning.fid(), fjernOrgtilknytning);
    }

    private void settTypeEksternSamarbeidspartnerHvisSkatt(OrgtilknytningState orgtilknytning, InputMetadata metadata) {
        if (erTypeEksternSamarbeidspartner(orgtilknytning)) return; // Er allerede ekstern samarbeidspartner

        IOrgenhet orgenhet = orgenhetService.getById(orgtilknytning.orgEnhetId()).orElse(null);
        if (erUnderSkatteetatenNiv0Enhet(orgenhet)) {
            settTilknytningtype(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER, orgtilknytning.fid(), orgtilknytning.navident(), metadata);
        }
    }

    private static boolean erTypeEksternSamarbeidspartner(OrgtilknytningState orgtilknytning) {
        return Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER.equals(orgtilknytning.orgtilknytningType().orElse(null));
    }
}
