package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OrganiseringStateRepositoryTest {


    @DisplayName("Test getActiveOrganisering")
    @ParameterizedTest
    @MethodSource("getTestParameters")
    void test_getActiveOrganisering(OrganiseringState organiseringsState, int expectedNumberOfActiveOrganisering) {
        OrganiseringStateRepository repository = new OrganiseringStateRepository();
        repository.put(organiseringsState.fid(), organiseringsState);

        assertThat(repository.getActiveOrganisering().size()).isEqualTo(expectedNumberOfActiveOrganisering);
    }

    private static Stream<Arguments> getTestParameters() {
        OrganiseringState organisering_same_fom_tom = OrganiseringState.builder()
            .fid("organisering_1_fid")
            .id("organisering_1_id")
            .idOver("organisering_1_idOver")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now()))
            .build();

        OrganiseringState organisering_tom_today_minus_2_dager = OrganiseringState.builder()
            .fid("organisering_tom_today_minus_2_dager_fid")
            .id("organisering_tom_today_minus_2_dager_id")
            .idOver("organisering_tom_today_minus_2_dager_idOver")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now().minusDays(2)))
            .build();

        OrganiseringState organisering_tom_today_plus_2_dager = OrganiseringState.builder()
            .fid("organisering_tom_today_plus_2_dager_fid")
            .id("organisering_tom_today_plus_2_dager_id")
            .idOver("organisering_tom_today_plus_2_dager_idOver")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusDays(2)))
            .build();

        return Stream.of(
            Arguments.of(organisering_same_fom_tom, 0),
            Arguments.of(organisering_tom_today_minus_2_dager, 0),
            Arguments.of(organisering_tom_today_plus_2_dager, 1)
        );
    }
}
