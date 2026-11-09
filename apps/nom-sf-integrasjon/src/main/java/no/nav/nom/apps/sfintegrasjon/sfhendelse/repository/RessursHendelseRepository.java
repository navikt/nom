package no.nav.nom.apps.sfintegrasjon.sfhendelse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RessursHendelseRepository extends JpaRepository<RessursHendelse, Long> {

    @Query(
        value = """
                SELECT * 
                FROM ressurs_hendelse 
                WHERE behandlings_id IN :behandlingsIder
                LIMIT 1000
                """, nativeQuery = true
    )
    List<RessursHendelse> getUbehandledeHendelser(List<Long> behandlingsIder);

    @Query(
        value = """
                SELECT DISTINCT ON (navident) *
                FROM ressurs_hendelse
                WHERE behandlings_id IS NULL
                ORDER BY id;
                """,
        nativeQuery = true
    )
    List<RessursHendelse> getSingleMottattHendelsePerNavident();
}
