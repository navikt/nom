package no.nav.nom.apps.api.orgtilknytning.job;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.today;

@Component
@Slf4j
public class OTUtenforHierarki {
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final LeaderElection leaderElection;

    private final HashMap<OrgtilknytningState, IOrgenhet> otUtenforHierarkieMedAlternativOrgenhet = new HashMap<>();

    public OTUtenforHierarki(OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService,
                             LeaderElection leaderElection) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.leaderElection = leaderElection;
    }

    public void findOTUtenforHierarkiFromDataLast() {
        findOTUtenforHierarki();

        autoflyttOTUtenforHierarki();

        findOTUtenforHierarki();
        alertForOTUtenforHierarki();
    }

    public void findOTUtenforHierarki() {
        otUtenforHierarkieMedAlternativOrgenhet.clear();

        var aktiveOrgenheter = orgenhetService.getAllEnheterMedOrganisering();
        var aktiveOrgenhetIder = aktiveOrgenheter.stream().map(IOrgenhet::getId).toList();
        var oterUtenforHierarki = orgtilknytningService.getAll().stream()
            .filter(ot -> !aktiveOrgenhetIder.contains(ot.orgEnhetId()))
            .filter(ot -> likEllerEtter(ot.gyldigTom().orElse(null), today()))
            .toList();

        oterUtenforHierarki.forEach(ot -> {
            var alternativOrgenhet = finnAlternativOrgenhet(ot, aktiveOrgenheter);
            otUtenforHierarkieMedAlternativOrgenhet.put(ot, alternativOrgenhet);
        });
        log.info("Fant {} OTer utenfor aktiv hierarki", otUtenforHierarkieMedAlternativOrgenhet.size());
    }

    private void autoflyttOTUtenforHierarki() {
        // grupper på til enhet
        // finn til enhets 'organisering' gyldigFom

    }

    // Dette fordi vi ønsker å bruke datoen OTen havnet utenfor hierarkiet.
    // Ved rekjøringer vil dette være stabilt
    // Og ny orgenhet gyldigFom kan være før dette
    private LocalDate getEnhetsOrganiseringGyldigFom(String orgenhetId) {
        throw new UnsupportedOperationException("Ikke implementert");
    }

    private IOrgenhet finnAlternativOrgenhet(OrgtilknytningState ot, Collection<IOrgenhet> aktiveOrgenheter) {
        var faktiskOrgenhet = orgenhetService.getById(ot.orgEnhetId()).orElse(null);
        if (faktiskOrgenhet == null) {
            log.error("Fant ikke orgenhet for Orgenhet med id {} for OT {}", ot.orgEnhetId(), ot);
            return null;
        }
        return aktiveOrgenheter.stream()
            .filter(oe -> oe.getAgressoId().equals(faktiskOrgenhet.getAgressoId()))
            .filter(oe -> oe.getOrgNiv().equals(faktiskOrgenhet.getOrgNiv()))
            .findFirst().orElse(null);
    }


    private void alertForOTUtenforHierarki() {
        boolean sendAlert = otUtenforHierarkieMedAlternativOrgenhet.keySet().stream().anyMatch(ot -> ot.gyldigFom().isBefore(LocalDate.now().plusWeeks(1)));
        if (sendAlert && leaderElection.isLeader()) {
            log.error("Orgtilknytninger utenfor aktiv hierarki");
            log.error("Detaljer{}", toLogg(otUtenforHierarkieMedAlternativOrgenhet));
        }
    }

    private String toLogg(HashMap<OrgtilknytningState, IOrgenhet> oterUtenforHierarki) {
        var logg = new StringBuilder();
        oterUtenforHierarki.forEach((ot, orgenhet) -> {
            logg.append("   Orgtilknytning: ").append(ot).append("\n");
            if (orgenhet == null) {
                logg.append("      Fant ikke alternativ orgenhet\n");
            } else {
                logg.append("      Alternativ orgenhet: ").append(orgenhet.getId()).append(" ").append(orgenhet.getNavn()).append("\n");
            }
        });
        return logg.toString();
    }

    public HashMap<OrgtilknytningState, IOrgenhet> getOtUtenforHierarkiMedAlternativOrgenhet() { return otUtenforHierarkieMedAlternativOrgenhet; }
}
