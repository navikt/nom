package no.nav.nom.apps.nom.enheter.enhet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganiseringRepository extends JpaRepository<Organisering, Long> {

    Organisering findByOverenhetAndUnderenhetAndOrganiseringstypeAndGyldigTilIsNull(Long overEnhet, Long underEnhet, String organiseringstype);

}
