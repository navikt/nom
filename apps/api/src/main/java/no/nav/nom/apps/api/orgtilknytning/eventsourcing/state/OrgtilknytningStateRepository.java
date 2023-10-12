package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrgtilknytningStateRepository {
    public static final String UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE = "nom.eksponer_orgtilknytninger_for_eksterne";
    private final Unleash unleash;
    private final RessursStateRepository ressursStateRepository;
    private final Map<String, OrgtilknytningState> orgtilknytningByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgtilknytningState>> orgtilknytningerByNavident = new ConcurrentHashMap<>();

    private final Map<String, OrgtilknytningState> orgtilknytningByFidEksklusiveEksterne = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgtilknytningState>> orgtilknytningerByNavidentEksklusiveEksterne = new ConcurrentHashMap<>();
    private final RessursStateListener ressursStateListener;

    public OrgtilknytningStateRepository(Unleash unleash, RessursStateRepository ressursStateRepository, RessursStateListener ressursStateListener) {
        this.unleash = unleash;
        this.ressursStateRepository = ressursStateRepository;
        this.ressursStateListener = ressursStateListener;
    }

    public Optional<OrgtilknytningState> findByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
            return Optional.ofNullable(orgtilknytningByFid.get(fid));
        } else {
            return Optional.ofNullable(orgtilknytningByFidEksklusiveEksterne.get(fid));
        }
    }

    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
        if (unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
            return orgtilknytningByFid.entrySet().stream()
                .filter(e -> fids.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        } else {
            return orgtilknytningByFidEksklusiveEksterne.entrySet().stream()
                .filter(e -> fids.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        }
    }

    public Collection<OrgtilknytningState> findByNavident(String navident) {
        if (unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
            return orgtilknytningerByNavident.getOrDefault(navident, Collections.emptyMap()).values();
        } else {
            return orgtilknytningerByNavidentEksklusiveEksterne.getOrDefault(navident, Collections.emptyMap()).values();
        }
    }

    public Collection<OrgtilknytningState> findByNavident(String navident, boolean hentForEksterneUavhengigAvFeatureFlag) {
        if (hentForEksterneUavhengigAvFeatureFlag) {
            return orgtilknytningerByNavident.getOrDefault(navident, Collections.emptyMap()).values();
        } else {
            return findByNavident(navident);
        }
    }

    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
        if (unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
            return orgtilknytningerByNavident.entrySet().stream()
                .filter(e -> navidents.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(identTilknytninger -> identTilknytninger.values().stream())
                .collect(Collectors.toList());
        } else {
            return orgtilknytningerByNavidentEksklusiveEksterne.entrySet().stream()
                .filter(e -> navidents.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(identTilknytninger -> identTilknytninger.values().stream())
                .collect(Collectors.toList());
        }
    }

    public Collection<OrgtilknytningState> getAll() {
        if (unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
            return orgtilknytningByFid.values();
        } else {
            return orgtilknytningByFidEksklusiveEksterne.values();
        }
    }

    public int count() {
        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
            return orgtilknytningByFid.size();
        } else {
            return orgtilknytningByFidEksklusiveEksterne.size();
        }
    }

    public void putEventAggregate(String fid, OrgtilknytningState newOrgtilknytningState) {
        if (newOrgtilknytningState == null) {
            removeFromState(fid);
        } else {
            addToState(newOrgtilknytningState);
        }

        updateStateEksklusiveEksterne(fid, newOrgtilknytningState);
    }

    private void addToState(OrgtilknytningState newOrgtilknytningState) {
        orgtilknytningByFid.put(newOrgtilknytningState.fid(), newOrgtilknytningState);

        var navidentOrgtilknytninger = orgtilknytningerByNavident.get(newOrgtilknytningState.navident());
        if (navidentOrgtilknytninger == null) {
            navidentOrgtilknytninger = new ConcurrentHashMap<>();
            orgtilknytningerByNavident.put(newOrgtilknytningState.navident(), navidentOrgtilknytninger);
        }
        navidentOrgtilknytninger.put(newOrgtilknytningState.fid(), newOrgtilknytningState); // Sett inn ny orgtilknytning, eller bytt ut med ny state evt.
    }

    private void removeFromState(String fid) {
        var eksisterede = orgtilknytningByFid.get(fid);
        if (eksisterede != null) {
            var navidentOrgtilknytninger = orgtilknytningerByNavident.get(eksisterede.navident());
            if (navidentOrgtilknytninger != null) {
                navidentOrgtilknytninger.remove(fid);
            }
        }
        orgtilknytningByFid.remove(fid);
    }

    private void updateStateEksklusiveEksterne(String fid, OrgtilknytningState newOrgtilknytningState) {
        if (newOrgtilknytningState == null) {
            removeFromStateEksklusiveEksterne(fid);
        } else if (!getNavidenterForEksterne().contains(newOrgtilknytningState.navident())) { // Legg kun inn hvis ikke ekstern navident
            addToStateEkslusiveEksterne(newOrgtilknytningState);
        }
    }

    private void addToStateEkslusiveEksterne(OrgtilknytningState newOrgtilknytningState) {
        orgtilknytningByFidEksklusiveEksterne.put(newOrgtilknytningState.fid(), newOrgtilknytningState);

        var navidentOrgtilknytninger = orgtilknytningerByNavidentEksklusiveEksterne.get(newOrgtilknytningState.navident());
        if (navidentOrgtilknytninger == null) {
            navidentOrgtilknytninger = new ConcurrentHashMap<>();
            orgtilknytningerByNavidentEksklusiveEksterne.put(newOrgtilknytningState.navident(), navidentOrgtilknytninger);
        }
        navidentOrgtilknytninger.put(newOrgtilknytningState.fid(), newOrgtilknytningState); // Sett inn ny orgtilknytning, eller bytt ut med ny state evt.
    }

    private void removeFromStateEksklusiveEksterne(String fid) {
        var eksisterede = orgtilknytningByFidEksklusiveEksterne.get(fid);
        if (eksisterede != null) {
            var navidentOrgtilknytninger = orgtilknytningerByNavidentEksklusiveEksterne.get(eksisterede.navident());
            if (navidentOrgtilknytninger != null) {
                navidentOrgtilknytninger.remove(fid);
            }
        }
        orgtilknytningByFidEksklusiveEksterne.remove(fid);
    }

    private List<String> getNavidenterForEksterne() {
        // Kan ikke laste orgtilknytninger før alle ressurser er lastet initielt
        while (!ressursStateListener.initloadComplete()) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.error("Feil ved venting på at ressursStateListener skal bli ferdig med initload", e);
            }
        }
        List<String> eksterneNavidenter = ressursStateRepository.getAllBareEksterne().stream()
            .map(RessursState::navident)
            .collect(Collectors.toList());
        return eksterneNavidenter;
    }
}
