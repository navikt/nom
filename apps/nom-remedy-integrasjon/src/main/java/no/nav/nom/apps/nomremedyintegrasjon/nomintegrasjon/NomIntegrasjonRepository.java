package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NomIntegrasjonRepository extends JpaRepository<NomIntegrasjonEntity, String> {

}
