package no.nav.nom.apps.sfintegrasjon.organisasjon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface OrganisasjonHendelseRepository extends JpaRepository<OrganisasjonHendelse, Long> {

    @Query(
        value = "select distinct on (fid) * " +
                "from organisasjon_hendelse o " +
                "where mottatt_tidspunkt <= :mottattTidspunkt and behandlings_id is null",
        nativeQuery = true)
    List<OrganisasjonHendelse> getOrgranisasjonHendelserUtenStatus(LocalDateTime mottattTidspunkt);

    List<OrganisasjonHendelse> findAllByBehandlingsIdIn(List<Long> behandlingsIds);

    default Map<String, List<OrganisasjonHendelse>> findAllGroupedByOrgEnhetId(List<Long> behandlingsIds) {
        return findAllByBehandlingsIdIn(behandlingsIds).stream().collect(Collectors.groupingBy(OrganisasjonHendelse::getOrgEnhetId));
    }
}
