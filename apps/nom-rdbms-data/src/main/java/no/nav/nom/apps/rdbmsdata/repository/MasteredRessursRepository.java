package no.nav.nom.apps.rdbmsdata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasteredRessursRepository extends JpaRepository<MasteredRessurs, String> {}