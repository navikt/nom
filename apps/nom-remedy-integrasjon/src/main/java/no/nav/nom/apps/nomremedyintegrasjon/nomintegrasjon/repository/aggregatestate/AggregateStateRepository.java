package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AggregateStateRepository extends JpaRepository<AggregateState, UUID> {

    List<AggregateState> findAggregateStatesByMasseflyttingIsFalseOrMasseflyttingIsNullAndDelayedUntilIsLessThanEqual(LocalDate delayedUntil);

    @Query(
        nativeQuery = true, value =
        """
        SELECT * FROM aggregate_state
        WHERE created_at < (NOW() - INTERVAL '5 seconds')
        AND delayed_until IS NULL
        """
    )
    List<AggregateState> findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

    List<AggregateState> findByStateFid(String stateFid);

    List<AggregateState> findAggregateStatesByStateFidAndMasseflyttetFraFid(String stateFid, String masseflyttetFraFid);

    List<AggregateState> findAggregateStatesByNavidentAndMasseflyttetFraFidAndStateType(String navident, String masseflyttetFraFid, String stateType);
}
