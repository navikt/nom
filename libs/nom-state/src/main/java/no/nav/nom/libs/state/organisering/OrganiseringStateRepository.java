package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface OrganiseringStateRepository {
    Optional<OrganiseringState> findByFid(String fid);

    Optional<OrganiseringState> findActiveByFid(String fid);

    Optional<OrganiseringState> findActiveById(String id);

    List<OrganiseringState> findById(String id);

    void clear();

    void update(OrganiseringState organiseringState);

    int count();

    Map<String, OrganiseringState> getActiveOrganisering();

    List<OrganiseringState> getOrganiseringer();

    List<OrganiseringState> getOrganiseringerContainingId(String id);

    List<OrganiseringState> getOrganiseringerContainingId(String id, boolean kunAktive);

    Map<String, OrganiseringState> getActiveOrganiseringInBulk(Set<String> ids);
    
    boolean orgEnhetHasParent(String id);
    
    boolean orgEnhetHasChildren(String id);

    Set<OrganiseringState> getEnhetOgOrganiseringerNedover(String id);

    Set<OrganiseringState> getEnhetOgOrganiseringerOppover(String id);

    Set<String> getChildrenEnhetIdsFlattened(String id);

    Set<String> getParentEnhetIdsFlattened(String id);
    
    Set<String> getAlleLovnodeIder();

    Set<OrganiseringState> getLovnoderUnder(String id);

    Set<String> getLovnoderIderUnder(String id);

    boolean isActive(OrganiseringState organiseringState);
}
