package no.nav.nom.apps.api.domain;

import lombok.Value;
import lombok.val;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;

@Value
@Service
public class RessursRepository {

    ConcurrentMap<UUID, Ressurs> alleRessurserById = new ConcurrentHashMap<>();
    ConcurrentMap<String, Ressurs> alleRessurserByPersonIdent = new ConcurrentHashMap<>();
    ConcurrentMap<String, Ressurs> alleRessurserByNavIdent = new ConcurrentHashMap<>();
    AtomicInteger ressurserGauge;

    public Optional<Ressurs> findById(UUID uuid) {
        return Optional.ofNullable(alleRessurserById.get(uuid));
    }

    public Optional<Ressurs> findByPersonIdent(String ident) {
        return Optional.ofNullable(alleRessurserByPersonIdent.get(ident));
    }

    public Optional<Ressurs> findByNavIdent(String navIdent) {
        return Optional.ofNullable(alleRessurserByNavIdent.get(navIdent));
    }

    public List<Ressurs> findByPersonIdentIn(List<String> idents) {
        return idents.stream().map(alleRessurserByPersonIdent::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Ressurs> findByNavIdentIn(List<String> navIdents) {
        return navIdents.stream().map(alleRessurserByNavIdent::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Ressurs> findAllByIdsIn(Set<UUID> uuids) {
        return uuids.stream()
                .map(alleRessurserById::get)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableList());
    }

    public void save(List<HendelseTo> hendelser) {
        hendelser.stream()
                .collect(groupingBy(e -> UUID.fromString(e.getData().get("fid").asText())))
                .forEach((key, value) -> {
                    findById(key).ifPresentOrElse(current -> {
                        val newRessurs = current.append(value);
                        add(newRessurs);
                    }, () -> add(new Ressurs(value)));
                });
    }

    private void add(Ressurs ressurs) {
        var prev = alleRessurserById.get(ressurs.getId());
        if (prev != null) {
            alleRessurserByPersonIdent.remove(ressurs.getPersonIdent());
            alleRessurserByNavIdent.remove(ressurs.getNavIdent());
        }

        alleRessurserById.put(ressurs.getId(), ressurs);
        alleRessurserByPersonIdent.put(ressurs.getPersonIdent(), ressurs);
        alleRessurserByNavIdent.put(ressurs.getNavIdent(), ressurs);
        ressurserGauge.set(alleRessurserById.size());
    }

    public Collection<Ressurs> getAlleRessurser() {
        return alleRessurserById.values();
    }

    public void clearRepository() {
        alleRessurserById.clear();
        alleRessurserByPersonIdent.clear();
        alleRessurserByNavIdent.clear();
        ressurserGauge.set(0);
    }
}
