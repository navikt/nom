package no.nav.nom.libs.state.orgenhetleder;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrgEnhetLederStateRepository {

    Optional<OrgEnhetLederState> findByFid(String fid);

    List<OrgEnhetLederState> findAllByNavident(String navident);

    List<OrgEnhetLederState> findAllByOrgEnhetId(String orgEnhetId);

    Optional<OrgEnhetLederState> getActiveForOrgEnhet(String orgEnhetId);

    boolean hasActiveInPeriodByOrgEnhetId(String orgEnhetId, LocalDate startdato, LocalDate sluttdato);

    int count();

    void update(String fid, OrgEnhetLederState state);

    void clear();
}
