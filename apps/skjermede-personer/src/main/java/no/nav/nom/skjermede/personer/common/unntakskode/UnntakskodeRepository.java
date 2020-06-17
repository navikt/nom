package no.nav.nom.skjermede.personer.common.unntakskode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnntakskodeRepository extends JpaRepository<Unntakskode, Long> {
    @Query(value = "SELECT DISTINCT KODE FROM T_UNNTAKSKODE", nativeQuery = true)
    List<String> findKoder();
}
