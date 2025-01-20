package no.nav.nom.apps.api.orgtilknytning.u4diff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface U4DiffRepository extends JpaRepository<U4Diff, Integer> {
    List<U4Diff> findByNavident(String navident);
    List<U4Diff> findByType(U4DiffType type);

    Long deleteByNavident(String name);
}
