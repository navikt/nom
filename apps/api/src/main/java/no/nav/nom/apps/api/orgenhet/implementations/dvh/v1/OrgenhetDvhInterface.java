package no.nav.nom.apps.api.orgenhet.implementations.dvh.v1;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.DvhInterface;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgenhetDvhInterface implements DvhInterface {

    private final DvhRepository dvhRepository;

    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        return Optional.ofNullable(dvhRepository.getEnheterByNivaByAgressoId().containsKey(orgNiv) ? dvhRepository.getEnheterByNivaByAgressoId().get(orgNiv).get(agressoId) : null);
    }

    public List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds) {
        return getAllEnheter().stream().filter(e -> agressoIds.contains(e.getAgressoId())).toList();
    }

    public Collection<IOrgenhet> getAllEnheter() {
        return dvhRepository.getEnheterByNivaByAgressoId().values().stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return getAllEnheter().stream()
                .filter(e -> e.getAgressoId().toUpperCase().startsWith(agressoId.toUpperCase()))
                .toList();
    }

    public List<IOrgenhet> matchByNavn(String navn) {
        return getAllEnheter().stream()
                .filter(e -> e.getNavn().toUpperCase().startsWith(navn.toUpperCase()))
                .toList();
    }
}
