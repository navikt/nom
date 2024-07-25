package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrganiseringStateRepository {
    Optional<OrganiseringState> findByFid(String fid);

    Optional<OrganiseringState> findActiveByFid(String fid);

    Optional<OrganiseringState> findActiveById(String id);

    Optional<OrganiseringState> findById(String id);

    void clear();

    void update(OrganiseringState organiseringState);

    void putEventAggregate(OrganiseringState organiseringState);
    void removeEventAggregate(OrganiseringState organiseringState);

    int count();

    void logRepositoryState();

    Map<String, OrganiseringState> getActiveOrganisering();

    List<OrganiseringState> getOrganiseringer();

    List<OrganiseringState> getOrganiseringerContainingId(String id);
}
