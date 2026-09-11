package no.nav.nom.apps.sfintegrasjon.behandling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehandlingRepository extends JpaRepository<Behandling, Long> {

    @Query(value = "SELECT nextval('behandlings_id')",nativeQuery = true)
    Long getBehandlingsId();

    List<Behandling> getBehandlingByBehandlingsId(Long behandlingsId);

}
