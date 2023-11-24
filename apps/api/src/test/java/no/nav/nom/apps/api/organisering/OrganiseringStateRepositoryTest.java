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

    @DisplayName("Test ActiveOrganisering method")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getOrganiseringStates")
    void test_getActiveOrganisering(String ignoredDescription, OrganiseringState organiseringsState, int expectedNumberOfActiveOrganisering) {
        OrganiseringStateRepository repository = new OrganiseringStateRepository();
        repository.put(organiseringsState.fid(), organiseringsState);

        assertThat(repository.getActiveOrganisering().size())
            .isEqualTo(expectedNumberOfActiveOrganisering);
    }

    private static Stream<Arguments> getOrganiseringStates() {

        var IS_ACTIVE = "is active";
        var IS_NOT_ACTIVE = "is not active";

        OrganiseringState organisering_fom_tom_today = OrganiseringState.builder()
            .fid("organisering_fom_tom_today" + "_fid")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now()))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_todayMinus2_dager = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_todayMinus2_dager" + "_fid")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now().minusDays(2)))
            .build();

        OrganiseringState organisering_fom_today_tom_todayPlus2_dager = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_todayPlus2_dager" + "_fid")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusDays(2)))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_today = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_today" + "_fid")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now()))
            .build();

        OrganiseringState organisering_fom_today_tom_todayMinus1 = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_todayMinus1" + "_fid")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_todayPlus10 = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_todayPlus10" + "_fid")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(10)))
            .build();

        OrganiseringState organisering_fom_today_tom_notset = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_notset" + "_fid")
            .gyldigFom(LocalDate.now())
            .build();

        OrganiseringState organisering_fom_today_tom_empty = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_empty" + "_fid")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.empty())
            .build();

        return Stream.of(
            Arguments.of("organisering_fom_tom_today " + IS_NOT_ACTIVE, organisering_fom_tom_today, 0),
            Arguments.of("organisering_fom_todayMinus10_tom_todayMinus2_dager is " + IS_NOT_ACTIVE, organisering_fom_todayMinus10_tom_todayMinus2_dager, 0),
            Arguments.of("organisering_fom_today_tom_todayMinus1 " + IS_NOT_ACTIVE, organisering_fom_today_tom_todayMinus1, 0),
            Arguments.of( "organisering_fom_today_tom_todayPlus2_dager " + IS_ACTIVE, organisering_fom_today_tom_todayPlus2_dager, 1),
            Arguments.of("organisering_fom_todayMinus10_tom_today " + IS_ACTIVE, organisering_fom_todayMinus10_tom_today, 1),
            Arguments.of("organisering_fom_todayMinus10_tom_todayPlus10 " + IS_ACTIVE, organisering_fom_todayMinus10_tom_todayPlus10, 1),
            Arguments.of("organisering_fom_today_tom_notset " + IS_ACTIVE, organisering_fom_today_tom_notset, 1),
            Arguments.of("organisering_fom_today_tom_empty " + IS_ACTIVE, organisering_fom_today_tom_empty, 1)
        );
    }
}
