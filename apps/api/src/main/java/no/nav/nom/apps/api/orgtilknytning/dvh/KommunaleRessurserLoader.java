package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.ShadowOrgtilknytningerKommunaleRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Denne lager ingen faktiske orgtilknytninger
 * Den parser om kommunale ressurser mangler stillinger i Unit4 og smatidig ikke har en OT i NOM (e.g. mastret)
 * Så vil den benytte seg av DVH sin OrgenhetPaaRessurs info som er en best effort tolkning av Remedy enhet etc for å plassere i organisasjonen.
 * Og lage en liksom OT - som kun tas vare på i ShadowOrgtilknytningerKommunaleRepository
 * NOM bruker aldri denne infoen selv, men den leveres til Omada, slik at Omada får tilknytning på 'aktive' Kommmunale ressurser som (enda) ikke har fått noen Stilling i Unit4
 * Når NOM blir full master for alle kommunale så kan dette fjernes
 */
@Slf4j
@Service
public class KommunaleRessurserLoader {
    private final KommunaleRessurserProcessor kommunaleRessurserProcessor;
    private final RessursStillingLoader ressursStillingLoader;
    private final RessursStillingStrukturerer ressursStillingStrukturerer;
    private final ShadowOrgtilknytningerKommunaleRepository shadowOrgtilknytningerKommunaleRepository;
    private final RessursStateRepository ressursStateRepository;
    private final DvhOrdsService dvhOrdsService;

    public KommunaleRessurserLoader(
        KommunaleRessurserProcessor kommunaleRessurserProcessor,
        RessursStillingLoader ressursStillingLoader,
        RessursStillingStrukturerer ressursStillingStrukturerer,
        ShadowOrgtilknytningerKommunaleRepository shadowOrgtilknytningerKommunaleRepository,
        RessursStateRepository ressursStateRepository, DvhOrdsService dvhOrdsService
    ) {
        this.kommunaleRessurserProcessor = kommunaleRessurserProcessor;
        this.ressursStillingLoader = ressursStillingLoader;
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.shadowOrgtilknytningerKommunaleRepository = shadowOrgtilknytningerKommunaleRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.dvhOrdsService = dvhOrdsService;
    }

    public void loadAndProcess() {
        log.info("Starter last og behandling av kommunale ressurser og orgtilknytninger");
        LocalDateTime start = LocalDateTime.now();

        var aktiveKommunaleNavidenterINom = getAktiveKommunaleNavidenter();

        List<DvhRessurs> dvhKommunaleRessurser = loadKommunaleForNavidenter(aktiveKommunaleNavidenterINom);
        List<String> kommunaleNavidenter = dvhKommunaleRessurser.stream().map(DvhRessurs::getNavIdent).toList();

        Map<String, List<StillingPeriode>> stillingPerioderPerNavident = loadStillinger(kommunaleNavidenter);

        log.info("{} kommunale ressurser med U4 stillinger", stillingPerioderPerNavident.size());

        Map<String, OrgtilknytningState> tmpKommunaleOrgtilknytninger = new ConcurrentHashMap<>();
        dvhKommunaleRessurser.forEach(r -> {
            var tmpOT = kommunaleRessurserProcessor.process(r, stillingPerioderPerNavident.get(r.getNavIdent()));
            tmpOT.ifPresent(orgtilknytningState -> tmpKommunaleOrgtilknytninger.put(r.getNavIdent(), orgtilknytningState));
        });

        shadowOrgtilknytningerKommunaleRepository.resetWithNewData(tmpKommunaleOrgtilknytninger);

        log.info(
            "last og behandling av kommunale ressurser og orgtilknytninger. Har laget {} temporære kommunale Orgtilknytninger. Tok {}",
            tmpKommunaleOrgtilknytninger.size(),
            durationAsH24(start)
        );
    }

    private List<String> getAktiveKommunaleNavidenter() {
        return ressursStateRepository.getAll().stream()
            .filter(r -> (r.startdato() == null || r.startdato().isBefore(LocalDate.now()))
                && (r.sluttdato() == null || r.sluttdato().isAfter(LocalDate.now()))
            )
            .filter(r -> r.sektor().equals(Sektor.NAV_KOMMUNAL))
            .map(RessursState::navident)
            .toList();
    }

    private Map<String, List<StillingPeriode>> loadStillinger(List<String> kommunaleNavidenter) {
        List<DvhRessursStilling> kommunaleRessursStilllinger =
            ressursStillingLoader.load().stream()
                .filter(rs -> rs.getNavident().isPresent() && kommunaleNavidenter.contains(rs.getNavident().get()))
                .toList();

        return ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(kommunaleRessursStilllinger);
    }

    List<DvhRessurs> loadKommunaleForNavidenter(List<String> aktiveKommunaleNavidenter) {
        LocalDateTime start = LocalDateTime.now();

        var dvhKommunaleRessurser = dvhOrdsService.getKommunaleRessurser(aktiveKommunaleNavidenter);

        log.info("Lastet {} Dvh Kommunale Ressurser. Tok {}", dvhKommunaleRessurser.size(), durationAsH24(start));
        return dvhKommunaleRessurser;
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
