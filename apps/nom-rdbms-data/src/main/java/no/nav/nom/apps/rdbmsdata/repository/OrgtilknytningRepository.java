package no.nav.nom.apps.rdbmsdata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgtilknytningRepository extends JpaRepository<Orgtilknytning, String> {}