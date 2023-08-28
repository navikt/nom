package no.nav.nom.apps.api.orgenhet;

import
    no.nav.nom.contract.nom.state.OrgEnhetState.OrgEnhetStateBuilder;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class OrgenhetTestUtils {

    public static OrgEnhetStateBuilder orgEnhetStateBuilderWithDefaultValues() {
        return OrgEnhetState.builder()
            .fid(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .navn(UUID.randomUUID().toString())
            .uniqueAgressoId(Optional.of(UUID.randomUUID().toString()))
            .agressoOrgNivaa(Optional.empty())
            .agressoId(Optional.of(UUID.randomUUID().toString()))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(1)))
            .orgEnhetsType(Optional.empty());

    }
}
