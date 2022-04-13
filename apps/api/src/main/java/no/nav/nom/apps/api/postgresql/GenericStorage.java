package no.nav.nom.apps.api.postgresql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GenericStorage extends JpaRepository<GenericStorageElement, UUID> {

}