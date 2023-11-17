package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NomIntegrasjonRepository extends JpaRepository<NomIntegrasjonEntity, String> {

    Optional<NomIntegrasjonEntity> findTopByOrderByEntryIdDesc();

    Optional<NomIntegrasjonEntity> findTopByNavIdEqualsOrderByEntryIdDesc(String navId);

}
