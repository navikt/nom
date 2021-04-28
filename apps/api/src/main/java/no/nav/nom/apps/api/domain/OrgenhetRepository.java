package no.nav.nom.apps.api.domain;

import lombok.Value;
import lombok.val;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;

@Value
@Service
public class OrgenhetRepository {

    ConcurrentMap<UUID, Orgenhet> alleOrgEnhetById = new ConcurrentHashMap<>();
    ConcurrentMap<String, Orgenhet> alleOrgEnhetByAgressoId = new ConcurrentHashMap<>();
    AtomicInteger enheterGauge;

    public Optional<Orgenhet> findById(UUID uuid) {
        return Optional.ofNullable(alleOrgEnhetById.get(uuid));
    }

    public Optional<Orgenhet> findByAgressoId(String agressoId) {
        return Optional.ofNullable(alleOrgEnhetByAgressoId.get(agressoId));
    }

    public List<Orgenhet> findAllByAgressoIdsIn(Set<String> uuids) {
        return uuids.stream()
                .map(alleOrgEnhetByAgressoId::get)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableList());
    }

    public List<Orgenhet> findAllByIdsIn(Set<UUID> uuids) {
        return uuids.stream()
                .map(alleOrgEnhetById::get)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableList());
    }

    public void save(List<HendelseTo> hendelser) {
        hendelser.stream()
                .collect(groupingBy(e -> UUID.fromString(e.getData().get("fid").asText())))
                .forEach((key, value) -> {
                    findById(key).ifPresentOrElse(current -> {
                        val newOrgEnhet = current.append(value);
                        add(newOrgEnhet);
                    }, () -> add(new Orgenhet(value)));
                });
    }

    private void add(Orgenhet enhet) {
        var prev = alleOrgEnhetById.get(enhet.getId());
        if (prev != null) {
            alleOrgEnhetByAgressoId.remove(prev.getAgressoId());
        }
        alleOrgEnhetById.put(enhet.getId(), enhet);
        alleOrgEnhetByAgressoId.put(enhet.getAgressoId(), enhet);
        enheterGauge.set(alleOrgEnhetById.size());
    }
}