package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ErrorStateRepository extends JpaRepository<ErrorState, UUID> {
    void deleteErrorStateByErrorStateFid(String errorStateFid);
}
