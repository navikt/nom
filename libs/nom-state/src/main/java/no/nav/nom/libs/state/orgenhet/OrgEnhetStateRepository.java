package no.nav.nom.libs.state.orgenhet;

import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrgEnhetStateRepository {
    Optional<OrgEnhetState> findByFid(String fid);

    List<OrgEnhetState> findByFidIn(List<String> fids);

    Optional<OrgEnhetState> findById(String id);

    List<OrgEnhetState> findByIdIn(List<String> ids);

    List<OrgEnhetState> findByIdIn(Set<String> ids);

    List<OrgEnhetState> findAllByUniqueAgressoId(String id);

    List<OrgEnhetState> findAllByAgressoIdIn(Set<String> agressoIds);

    List<OrgEnhetState> findAllByAgressoIdAndNiv(String id);

    List<OrgEnhetState> findAllByTilgangsenhetId(String tilgangsenhetId);

    Optional<OrgEnhetState> findNewestUniqueAgressoId(String id);

    Collection<OrgEnhetState> getAll();

    int count();

    void update(OrgEnhetState newOrgEnhetState);

    void clear();
}
