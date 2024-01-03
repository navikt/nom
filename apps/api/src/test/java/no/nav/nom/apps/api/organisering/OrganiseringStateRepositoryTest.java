package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


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

    @DisplayName("findActiveById should return OrganiseringState when the organisering is active")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getOrganiseringStates")
    void shouldGetActiveOrganiseringById(String ignoredDescription, OrganiseringState organiseringsState, int expectedNumberOfActiveOrganisering) {
        OrganiseringStateRepository repository = new OrganiseringStateRepository();
        repository.putEventAggregate(organiseringsState);

        if (expectedNumberOfActiveOrganisering == 0) {
            assertThat(repository.findActiveById(organiseringsState.id()))
                .isEqualTo(Optional.empty());
        } else {
            assertThat(repository.findActiveById(organiseringsState.id()))
                .isEqualTo(Optional.of(organiseringsState));
        }
    }

    private static Stream<Arguments> getOrganiseringStates() {

        var IS_ACTIVE = "is active";
        var IS_NOT_ACTIVE = "is not active";

        OrganiseringState organisering_fom_tom_today = OrganiseringState.builder()
            .fid("organisering_fom_tom_today" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now()))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_todayMinus2_dager = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_todayMinus2_dager" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now().minusDays(2)))
            .build();

        OrganiseringState organisering_fom_today_tom_todayPlus2_dager = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_todayPlus2_dager" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusDays(2)))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_today = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_today" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now()))
            .build();

        OrganiseringState organisering_fom_today_tom_todayMinus1 = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_todayMinus1" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_todayPlus10 = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_todayPlus10" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(10)))
            .build();

        OrganiseringState organisering_fom_today_tom_notset = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_notset" + "_fid")
            .id("the_id")
            .gyldigFom(LocalDate.now())
            .build();

        OrganiseringState organisering_fom_today_tom_empty = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_empty" + "_fid")
            .id("the_id")
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


    @Test
    @DisplayName("getOrganiseringerContainingId returnerer organiseringer med id enten i id eller idOver")
    void getOrganiseringerContainingId_should_return_organiseringer_with_id_both_in_id_and_idOver() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepository();

        OrganiseringState idInId = OrganiseringState.builder()
            .fid("1")
            .id(idToFind)
            .idOver("some_other_id")
            .build();

        OrganiseringState idInIdOver = OrganiseringState.builder()
            .fid("2")
            .id("a_different_id")
            .idOver(idToFind)
            .build();

        OrganiseringState notIncludingId = OrganiseringState.builder()
            .fid("3")
            .id("a_different_id")
            .idOver("some_other_id")
            .build();

        repository.putEventAggregate(idInId);
        repository.putEventAggregate(idInIdOver);
        repository.putEventAggregate(notIncludingId);


        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(2);
        assertThat(organiseringerReturned).containsAll(List.of(idInId, idInIdOver));
        assertThat(organiseringerReturned).doesNotContain(notIncludingId);
    }
}
