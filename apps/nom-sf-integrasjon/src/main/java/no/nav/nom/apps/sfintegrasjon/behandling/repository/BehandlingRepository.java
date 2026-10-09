package no.nav.nom.apps.sfintegrasjon.behandling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehandlingRepository extends JpaRepository<Behandling, Long> {

    @Query(value = "SELECT nextval('behandlings_id')", nativeQuery = true)
    Long getNextBehandlingsId();

    List<Behandling> getBehandlingByBehandlingsId(Long behandlingsId);

    @Query(
        value = """
                WITH siste_status AS (
                    SELECT DISTINCT ON (behandlings_id)
                    id, behandlings_id, outbox_sporings_id, status, behandling_tidspunkt
                    FROM behandling
                    order by behandlings_id, behandling_tidspunkt DESC, id DESC
                )
                SELECT ss.id, ss.behandlings_id, ss.outbox_sporings_id, ss.status, ss.behandling_tidspunkt
                FROM siste_status ss
                WHERE ss.status IN ('MOTTATT', 'FEILET')
                ORDER BY ss.behandling_tidspunkt, id
                """, nativeQuery = true
    )
    List<Behandling> getLatestNonProcessedBehandlinger();
}
