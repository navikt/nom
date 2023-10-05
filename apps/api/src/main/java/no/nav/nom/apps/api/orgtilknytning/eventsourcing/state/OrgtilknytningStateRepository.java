package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.Sektor;
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
    private final IRessursService ressursService;
    private final Map<String, OrgtilknytningState> orgtilknytningByFid = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OrgtilknytningState>> orgtilknytningerByNavident = new ConcurrentHashMap<>();

    public OrgtilknytningStateRepository(Unleash unleash, IRessursService ressursService) {
        this.unleash = unleash;
        this.ressursService = ressursService;
    }

    public Optional<OrgtilknytningState> findByFid(String fid) {
//        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
//            List<String> eksterneNavidenter = getNavidenterForEksterne();
//
//            return Optional.ofNullable(orgtilknytningByFid.get(fid));
//        } else {
            return Optional.ofNullable(orgtilknytningByFid.get(fid));
//        }
    }

    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
//        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
//            List<String> eksterneNavidenter = getNavidenterForEksterne();
//
//            return orgtilknytningByFid.entrySet().stream()
//                .filter(e -> fids.contains(e.getKey()))
//                .map(Map.Entry::getValue)
//                .filter(e -> !eksterneNavidenter.contains(e.navident()))
//                .collect(Collectors.toList());
//        } else {
            return orgtilknytningByFid.entrySet().stream()
                .filter(e -> fids.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
//        }
    }

    public Collection<OrgtilknytningState> findByNavident(String navident) {
//        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
//            List<String> eksterneNavidenter = getNavidenterForEksterne();
//
//            if (eksterneNavidenter.contains(navident)) {
//                return Collections.emptyList();
//            }
//        }
        return orgtilknytningerByNavident.getOrDefault(navident, Collections.emptyMap()).values();
    }

    public Collection<OrgtilknytningState> findByNavident(String navident, boolean hentForEksterneUavhengigAvFeatureFlag) {
//        if (hentForEksterneUavhengigAvFeatureFlag) {
//            return orgtilknytningerByNavident.getOrDefault(navident, Collections.emptyMap()).values();
//        } else {
            return findByNavident(navident);
//        }
    }

    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
//        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
//            List<String> eksterneNavidenter = getNavidenterForEksterne();
//
//            navidents.removeAll(eksterneNavidenter);
//        }
        return orgtilknytningerByNavident.entrySet().stream()
            .filter(e -> navidents.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .flatMap(identTilknytninger -> identTilknytninger.values().stream())
            .collect(Collectors.toList());
    }

    public Collection<OrgtilknytningState> getAll() {
//        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
//            List<String> eksterneNavidenter = getNavidenterForEksterne();
//
//            return orgtilknytningByFid.values().stream()
//                .filter(e -> !eksterneNavidenter.contains(e.navident()))
//                .collect(Collectors.toList());
//        } else {
            return orgtilknytningByFid.values();
//        }
    }

    public int count() {
//        if (!unleash.isEnabled(UNLEASH_EKSPONER_TILKNYTNINGER_FOR_EKSTERNE)) {
//            List<String> eksterneNavidenter = getNavidenterForEksterne();
//
//            return (int)orgtilknytningByFid.values().stream()
//                .filter(e -> !eksterneNavidenter.contains(e.navident()))
//                .count();
//        } else {
            return orgtilknytningByFid.size();
//        }
    }

    public void putEventAggregate(String fid, OrgtilknytningState newOrgtilknytningState) {
        if (newOrgtilknytningState == null) {
            var eksisterede = orgtilknytningByFid.get(fid);
            if (eksisterede != null) {
                var navidentOrgtilknytninger = orgtilknytningerByNavident.get(eksisterede.navident());
                if (navidentOrgtilknytninger != null) {
                    navidentOrgtilknytninger.remove(fid);
                }
            }
            orgtilknytningByFid.remove(fid);
        } else {
            orgtilknytningByFid.put(newOrgtilknytningState.fid(), newOrgtilknytningState);

            var navidentOrgtilknytninger = orgtilknytningerByNavident.get(newOrgtilknytningState.navident());
            if (navidentOrgtilknytninger == null) {
                navidentOrgtilknytninger = new ConcurrentHashMap<>();
                orgtilknytningerByNavident.put(newOrgtilknytningState.navident(), navidentOrgtilknytninger);
            }
            navidentOrgtilknytninger.put(newOrgtilknytningState.fid(), newOrgtilknytningState); // Sett inn ny orgtilknytning, eller bytt ut med ny state evt.
        }
    }

    private List<String> getNavidenterForEksterne() {
        List<String> eksterneNavidenter = ressursService.getAll().stream()
            .filter( r -> r.sektor().equals(Sektor.EKSTERN))
            .map(RessursState::navident)
            .toList();
        return eksterneNavidenter;
    }
}
