package no.nav.nom.apps.api.u4diff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface U4DiffRepository extends JpaRepository<U4Diff, Integer> {
    List<U4Diff> findByNavident(String navident);
    List<U4Diff> findByKildeAndNavident(U4DiffKilde kilde, String navident);
    List<U4Diff> findByType(U4DiffType type);
    List<U4Diff> findByKildeAndType(U4DiffKilde kilde, U4DiffType type);
    List<U4Diff> findByKilde(U4DiffKilde kilde);

    Long deleteByKildeAndNavident(U4DiffKilde kilde, String name);
    Long deleteByKildeAndType(U4DiffKilde kilde, U4DiffType type);
}
