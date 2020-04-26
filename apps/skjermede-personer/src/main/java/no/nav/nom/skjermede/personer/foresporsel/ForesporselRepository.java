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
    Optional<Foresporsel> findByFunksjonellIdAndBekreftetFalse(UUID funksjonellId);
    boolean existsByPersonidentAndBekreftetFalse(String personident);
    void deleteAllByEpostSendtBeforeOrBekreftetTrue(LocalDateTime before);
    Optional<Foresporsel> findByPersonidentAndBekreftetFalse(String personIdent);
}
