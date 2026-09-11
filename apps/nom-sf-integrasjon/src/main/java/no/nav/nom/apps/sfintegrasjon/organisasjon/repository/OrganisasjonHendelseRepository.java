package no.nav.nom.apps.sfintegrasjon.organisasjon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrganisasjonHendelseRepository extends JpaRepository<OrganisasjonHendelse, Long> {

    @Query(
        value = "select distinct on (fid) * " +
                "from organisasjon_hendelse o " +
                "where mottatt_tidspunkt <= :mottattTidspunkt and behandlings_id is null",
        nativeQuery = true)
    List<OrganisasjonHendelse> getOrgranisasjonHendelserUtenStatus(LocalDateTime mottattTidspunkt);
}
