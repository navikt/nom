package no.nav.nom.libs.state.orgtilknytning;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface OrgtilknytningStateRepository {
    enum DOLIs {
        TRUE,
        FALSE,
        ANY
    }

    Optional<OrgtilknytningState> findByFid(String fid);

    List<OrgtilknytningState> findByFidIn(List<String> fids);

    Collection<OrgtilknytningState> findByNavident(String navident);

    List<OrgtilknytningState> findByNavidentIn(List<String> navidents);

    Collection<OrgtilknytningState> getAll();

    List<String> getAktiveNavidenterPaaEnhet(String orgenhet);

    List<String> getAktiveNavidenterPaaEnhet(String orgenhet, DOLIs dolIs);

    List<String> getAktiveNavidenterPaaEnheter(List<String> orgEnheter);

    List<String> getAktiveEllerFremtidigeNavidenterPaaEnhet(String orgenhet);

    List<String> getAktiveEllerFremtidigeNavidenterPaaEnhet(String orgenhet, DOLIs dolIs);

    List<OrgtilknytningState> getOrgtilknytningerPaaEnhet(String orgenhet);

    List<OrgtilknytningState> getOrgtilknytningerPaaEnhet(String orgenhet, DOLIs dolIs);

    Map<String, List<OrgtilknytningState>> getOrgtilknytningerPaaEnheter(Set<String> orgenheter, DOLIs dolIs);

    int count();

    void update(String fid, OrgtilknytningState newOrgtilknytningState);

    void clear();
}
