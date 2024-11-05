package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganiseringStateRepositoryTest {

    private static final String ORG_ENHET = "ab123c";
    private static final String CHILD_ORG_ENHET = "de456f";
    private final OrganiseringStateRepository organiseringStateRepository = new OrganiseringStateRepositoryImpl();


    @Test
    public void should_return_false_when_parent_unit_is_missing() {
        var orgState = OrganiseringState.builder()
                .fid("random-fid")
                .id(ORG_ENHET)
                .build();
        organiseringStateRepository.putEventAggregate(orgState);

        assertThat(organiseringStateRepository.orgEnhetHasParent(ORG_ENHET)).isFalse();
    }

    @Test
    public void should_return_true_when_parent_unit_exists() {
        var orgState = OrganiseringState.builder()
                .fid("random-fid")
                .id(ORG_ENHET)
                .idOver("de456f")
                .build();
        organiseringStateRepository.putEventAggregate(orgState);

        assertThat(organiseringStateRepository.orgEnhetHasParent(ORG_ENHET)).isTrue();
    }

    @Test
    public void should_return_false_when_parent_unit_field_contains_empty_string() {
        var orgState = OrganiseringState.builder()
                .fid("random-fid")
                .id(ORG_ENHET)
                .idOver("")
                .build();
        organiseringStateRepository.putEventAggregate(orgState);

        assertThat(organiseringStateRepository.orgEnhetHasParent(ORG_ENHET)).isFalse();
    }

    @Test
    void should_return_true_for_organisering_with_children() {
        var parent = OrganiseringState.builder()
                .fid("fid-parent")
                .id(ORG_ENHET)
                .idOver("")
                .build();
        organiseringStateRepository.putEventAggregate(parent);

        var child = OrganiseringState.builder()
                .fid("fid-child")
                .id(CHILD_ORG_ENHET)
                .idOver(ORG_ENHET)
                .build();
        organiseringStateRepository.putEventAggregate(child);
        
        assertThat(organiseringStateRepository.orgEnhetHasChildren(ORG_ENHET)).isTrue();
    }

    @Test
    void should_return_false_for_organisering_with_no_children() {
        var parent = OrganiseringState.builder()
                .fid("fid-parent")
                .id(ORG_ENHET)
                .idOver("")
                .build();
        organiseringStateRepository.putEventAggregate(parent);

        assertThat(organiseringStateRepository.orgEnhetHasChildren(ORG_ENHET)).isFalse();
    }
}
