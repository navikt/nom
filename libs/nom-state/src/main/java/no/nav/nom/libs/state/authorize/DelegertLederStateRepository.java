package no.nav.nom.libs.state.authorize;

import no.nav.nom.contract.nom.state.LederTilgangState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DelegertLederStateRepository {
    Optional<LederTilgangState> findByFid(String fid);

    List<LederTilgangState> findByNavident(String navident);

    List<LederTilgangState> findDelegertAvNavident(String navident);

    List<String> aktiveDelegeringerForOrgenheterTilNavident(String navident);

    boolean erOrgenhetDelegertTilNavident(String orgenhetId, String navident);

    Optional<LederTilgangState> lederTilgangForOrgenhetOgNavident(String orgenhetId, String navident);

    Collection<LederTilgangState> getAll();

    int count();

    void update(LederTilgangState state);

    void clear();
}
