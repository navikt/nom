package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.DvhInterface;
import no.nav.nom.contract.nom.api.graphql.Kode;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@Service
@Profile("qa")
@RequiredArgsConstructor
public class OrgenhetDvhInterfaceV2 implements DvhInterface {

    private final DvhRepository dvhRepository;

    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        val result = dvhRepository.getOrgenhetV2().stream()
                .filter(e -> e.getAgressoId().equals(agressoId) && e.getOrgNiv().equals(orgNiv) && e.getNavn().equals(name))
                .toList();

        if(result.size() > 1){
            log.error("duplikat enhet i repository: "+result);
        }

        return result.size() == 1 ? Optional.ofNullable(result.get(0)) : Optional.empty();
    }

    public List<IOrgenhet> findEnheterByAgressoIdIn(List<String> agressoIds) {
        return getAllEnheter().stream().filter(e -> agressoIds.contains(e.getAgressoId())).toList();
    }

    public Collection<IOrgenhet> getAllEnheter() {
        return new ArrayList<>(dvhRepository.getOrgenhetV2());
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
