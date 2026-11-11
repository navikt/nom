package no.nav.nom.apps.sfintegrasjon.sfhendelse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SfHendelseBehandlingRepository extends JpaRepository<SfHendelseBehandling, Long> {

    List<SfHendelseBehandling> getSfHendelseBehandlingsByIdIsIn(List<Long> ids);

}
