package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganiseringStateRepositoryTest {

    private static final String ORG_ENHET = "ab123c";
    private final OrganiseringStateRepository organiseringStateRepository = new OrganiseringStateRepositoryImpl();


    @Test
    public void should_return_false_when_parent_unit_is_missing() {
        var orgState = OrganiseringState.builder()
                .fid("random-fid")
                .id(ORG_ENHET)
                .build();
        organiseringStateRepository.putEventAggregate(orgState);

        assertThat(organiseringStateRepository.hasOrganiseringParent(ORG_ENHET)).isFalse();
    }

    @Test
    public void should_return_true_when_parent_unit_exists() {
        var orgState = OrganiseringState.builder()
                .fid("random-fid")
                .id(ORG_ENHET)
                .idOver("de456f")
                .build();
        organiseringStateRepository.putEventAggregate(orgState);

        assertThat(organiseringStateRepository.hasOrganiseringParent(ORG_ENHET)).isTrue();
    }

    @Test
    public void should_return_false_when_parent_unit_field_contains_empty_string() {
        var orgState = OrganiseringState.builder()
                .fid("random-fid")
                .id(ORG_ENHET)
                .idOver("")
                .build();
        organiseringStateRepository.putEventAggregate(orgState);

        assertThat(organiseringStateRepository.hasOrganiseringParent(ORG_ENHET)).isFalse();
    }
}
