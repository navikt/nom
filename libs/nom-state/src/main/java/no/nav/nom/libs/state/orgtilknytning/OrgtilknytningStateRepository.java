package no.nav.nom.libs.state.orgtilknytning;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrgtilknytningStateRepository {

    Optional<OrgtilknytningState> findByFid(String fid);

    List<OrgtilknytningState> findByFidIn(List<String> fids);

    Collection<OrgtilknytningState> findByNavident(String navident);

    List<OrgtilknytningState> findByNavidentIn(List<String> navidents);

    Collection<OrgtilknytningState> getAll();

    int count();

    void update(OrgtilknytningState newOrgtilknytningState);

    void clear();
}
