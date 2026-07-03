package no.nav.nom.apps.sfintegrasjon.organisasjon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganisasjonHendelseRepository extends JpaRepository<OrganisasjonHendelse, Long> {
}
