package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class EksterneRessurserProcessor {

    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    public EksterneRessurserProcessor(RemedyEnhetMapperService remedyEnhetMapperService, OrgtilknytningStateService orgtilknytningStateService,
                                      OrgenhetService orgenhetService,
                                      OrgtilknytningCommandService orgtilknytningCommandService
    ) {
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

        var eksisterendeOrgtilknytninger = orgtilknytningStateService.getOrgtilknytningerByNavIdent(eksternRessurs.navident());

        if (eksisterendeOrgtilknytninger.isEmpty()) {
            log.debug("Oppretter orgtilknytning for ekstern ressurs {}", eksternRessurs.navident());
            opprettOrgtilknytning(eksternRessurs, orgenhet);
        } else {
            // Sjekk om det er ren match på orgtilknytningen - vi bryr oss kun om sluttdato, ikke startdato.
            var eksisterendeOrgtilknytning = eksisterendeOrgtilknytninger.stream()
                .filter(orgtilknytning -> orgtilknytning.orgEnhetId().equals(orgenhet.getId()) && orgtilknytning.gyldigTom().equals(eksternRessurs.remedySlutt()))
                .findFirst();
        }
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

    private void opprettOrgtilknytning(DvhEksternRessurs eksternRessurs, IOrgenhet orgenhet) {

    }
}
