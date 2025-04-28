package no.nav.nom.apps.api.orgtilknytning.job;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.MasseflyttOrgtilknytningerService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.today;

@Component
@Slf4j
public class OTUtenforHierarki {
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService;
    private final Unleash unleash;

    private final String UNLEASH_AUTO_FLYTT = "nom.otutenforhierarki.auto_flytt";

    private final HashMap<OrgtilknytningState, IOrgenhet> otUtenforHierarkieMedAlternativOrgenhet = new HashMap<>();
    private final HashMap<IOrgenhet, List<OrgtilknytningState>> perAlternativOrgenhet = new HashMap<>();

    private boolean ventPaaState = true;

    public OTUtenforHierarki(OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService,
                             OrganiseringStateRepository organiseringStateRepository, MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService,
                            Unleash unleash
    ) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.organiseringStateRepository = organiseringStateRepository;
        this.masseflyttOrgtilknytningerService = masseflyttOrgtilknytningerService;
        this.unleash = unleash;
    }

    public void findOTUtenforHierarkiFromDataLast() {
        findOTUtenforHierarki();

        autoflyttOTUtenforHierarki(perAlternativOrgenhet);

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
            if (alternativOrgenhet != null) {
                perAlternativOrgenhet.computeIfAbsent(alternativOrgenhet, k -> new java.util.ArrayList<>()).add(ot);
            }
        });
        log.info("Fant {} OTer utenfor aktiv hierarki", otUtenforHierarkieMedAlternativOrgenhet.size());
    }

    int autoflyttOTUtenforHierarki(Map<IOrgenhet, List<OrgtilknytningState>> perAlternativOrgenhet) {
        if (!unleash.isEnabled(UNLEASH_AUTO_FLYTT)) {
            return 0;
        }
        if (perAlternativOrgenhet.isEmpty()) {
            return 0;
        }
        log.info("Autoflytter OTer utenfor hierarki");

        AtomicInteger behandlet = new AtomicInteger();
        AtomicInteger feilet = new AtomicInteger();

        perAlternativOrgenhet.forEach((alternativOrgenhet, oter) -> {
            log.info("Autoflytter OTer utenfor hierarki for enhet {} - {} ", alternativOrgenhet.getId(), alternativOrgenhet.getNavn());
            var otFids = oter.stream().map(OrgtilknytningState::fid).toList();
            behandlet.addAndGet(otFids.size());

            var flyttedato = getEnhetsOrganiseringGyldigFom(alternativOrgenhet.getId());
            if ( flyttedato == null ) {
                log.error("Fant ikke organiseringens gyldigFom for alternativ orgenhet {}", alternativOrgenhet.getId());
                loggFeilede(otFids, "Klarte ikke sette flyttedato for alternativ orgenhet %s - %s".formatted(alternativOrgenhet.getId(), alternativOrgenhet.getNavn()));

                feilet.addAndGet(otFids.size());
                return;
            }

            // Så og si alle slike case så er firesifferenheten den samme, så flagget skal være false.
            boolean beholdTilganger = false;

            var inputMetadata = getInputMetadata(alternativOrgenhet, otFids, flyttedato);

            Map<OrgtilknytningState, String> feilede;
            try {
                feilede = masseflyttOrgtilknytningerService.flyttOrgtilknytninger(otFids, alternativOrgenhet.getId(), beholdTilganger, flyttedato, inputMetadata, ventPaaState);
                loggFeilede(feilede);
                feilet.addAndGet(feilede.size());
            } catch (Exception e) {
                log.error("Feil ved autoflytting av OTer utenfor hierarki for enhet {} - {} ", alternativOrgenhet.getId(), alternativOrgenhet.getNavn(), e);
                loggFeilede(otFids, "Feil ved autoflytting av OTer utenfor hierarki for enhet %s - %s".formatted(alternativOrgenhet.getId(), alternativOrgenhet.getNavn()));
                feilet.addAndGet(otFids.size());
            }
        });

        log.info("Ferdig med Autoflytting OTer utenfor hierarki {}/{} (feilede/toalt)", feilet.get(), behandlet.get());
        return feilet.get();
    }

    private void loggFeilede(List<String> otFids, String formatted) {
        HashMap<OrgtilknytningState, String> feilede = new HashMap<>();
        otFids.forEach(otFid -> {
            orgtilknytningService.findByFid(otFid).ifPresent(ot -> feilede.put(ot, formatted));
        });
        loggFeilede(feilede);
    }

    private void loggFeilede(Map<OrgtilknytningState, String> feilede) {
        if (feilede.isEmpty()) {
            return;
        }
        var antallFeilede = feilede.size();
        log.error("Fant {} OTer utenfor hierarki som ikke ble flyttet", antallFeilede);
        feilede.forEach((ot, msg) -> {
            log.error("Orgtilknytning {} {} {} [{} - {}]  - {}", ot.fid(), ot.navident(), ot.orgEnhetId(), ot.gyldigFom(), ot.gyldigTom(), msg);
        });
    }

    private static InputMetadata getInputMetadata(IOrgenhet alternativOrgenhet, List<String> otFids, LocalDate flyttedato) {
        return new InputMetadata(
            "nom-api OTUtenforHierarki",
            "autoflyttOTUtenforHierarki",
            Map.of("Orgtilknytning Fid's", otFids, "alternativ enhet", alternativOrgenhet, "flyttedato", flyttedato),
            new Origination("nom-api", "nom-api")
        );
    }

    // Dette fordi vi ønsker å bruke datoen OTen havnet utenfor hierarkiet.
    // Ved rekjøringer vil dette være stabilt
    // Og ny orgenhet gyldigFom kan være før dette
    private LocalDate getEnhetsOrganiseringGyldigFom(String orgenhetId) {
        return organiseringStateRepository
            .findById(orgenhetId)
            .map(OrganiseringState::gyldigFom).orElse(null);
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
        if (sendAlert) {
            log.error("Orgtilknytninger utenfor aktiv hierarki");
            log.error("Detaljer {}", toLogg(otUtenforHierarkieMedAlternativOrgenhet));
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

    public void setVentPaaState(boolean ventPaaState) {
        this.ventPaaState = ventPaaState;
    }
}
