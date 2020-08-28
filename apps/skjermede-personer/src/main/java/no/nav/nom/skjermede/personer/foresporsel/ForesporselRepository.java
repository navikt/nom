package no.nav.nom.skjermede.personer.foresporsel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForesporselRepository extends JpaRepository<Foresporsel, Long> {
    List<Foresporsel> findAllByEpostSendtNull();
    List<Foresporsel> findAllByTilknyttetRessurs(String tilknyttetRessurs);
    Optional<Foresporsel> findByTilknyttetRessursAndFornavnAndEtternavnAndBekreftetFalse(String tilknyttetRessurs, String fornavn, String etternavn);
    Optional<Foresporsel> findByFunksjonellIdAndBekreftetFalse(UUID funksjonellId);
    boolean existsByPersonidentAndBekreftetFalse(String personident);
    void deleteAllByOpprettetDatoBeforeOrBekreftetTrue(LocalDateTime before);
    Optional<Foresporsel> findByPersonidentAndBekreftetFalse(String personIdent);
    long countAllByBekreftetFalseAndEpostSendtIsNotNull();
    long countAllByEpostSendtIsNull();
}
