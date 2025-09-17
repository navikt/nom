package no.nav.nom.apps.api.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
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

    @Test
    @DisplayName("Test removeEventAggregate method")
    void test_removeEventAggregate_one_organiseringState() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        // Legg til en organisering
        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid("1")
            .id(idToFind)
            .idOver("a_random_id")
            .build();
        repository.update(organiseringState);

        // Sjekk at den er lagt til
        List<OrganiseringState> organiseringStateInState = repository.getOrganiseringerContainingId(idToFind);
        assertThat(organiseringStateInState).containsAll(List.of(organiseringState));

        // Fjern organiseringen fra state
        repository.removeEventAggregate(organiseringState);
        organiseringStateInState = repository.getOrganiseringerContainingId(idToFind);
        assertThat(organiseringStateInState.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Test removeEventAggregate method")
    void test_removeEventAggregate_more_organiseringStates() {
        final String id_1 = "id_1";
        final String id_2 = "id_2";
        final String id_3 = "id_3";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        // Legg til en organisering
        String fid_1 = "fid_1";
        String fid_2 = "fid_2";
        OrganiseringState organiseringState_1 = OrganiseringState.builder().fid(fid_1).id(id_1).idOver(id_3).build();
        OrganiseringState organiseringState_2 = OrganiseringState.builder().fid(fid_2).id(id_2).idOver(id_3).build();
        repository.update(organiseringState_1);
        repository.update(organiseringState_2);

        // Sjekk at den er lagt til
        List<OrganiseringState> organiseringStateInState = repository.getOrganiseringerContainingId(id_1);
        assertThat(organiseringStateInState).containsAll(List.of(organiseringState_1));
        organiseringStateInState = repository.getOrganiseringerContainingId(id_3);
        assertThat(organiseringStateInState).containsAll(List.of(organiseringState_1, organiseringState_2));

        // Fjern organiseringState_1 fra state
        repository.removeEventAggregate(organiseringState_1);
        organiseringStateInState = repository.getOrganiseringerContainingId(id_3);
        assertThat(organiseringStateInState.size()).isEqualTo(1);
        assertThat(organiseringStateInState).containsAll(List.of(organiseringState_2));
    }

    @DisplayName("Test ActiveOrganisering method")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getOrganiseringStates")
    void test_getActiveOrganisering(String ignoredDescription, OrganiseringState organiseringsState, int expectedNumberOfActiveOrganisering) {
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();
        repository.update(organiseringsState);

        assertThat(repository.getActiveOrganisering().size())
            .isEqualTo(expectedNumberOfActiveOrganisering);
    }

    @DisplayName("findActiveById should return OrganiseringState when the organisering is active")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getOrganiseringStates")
    void shouldGetActiveOrganiseringById(String ignoredDescription, OrganiseringState organiseringsState, int expectedNumberOfActiveOrganisering) {
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();
        repository.update(organiseringsState);

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
            .idOver("id_over")
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now())
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_todayMinus2_dager = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_todayMinus2_dager" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(LocalDate.now().minusDays(2))
            .build();

        OrganiseringState organisering_fom_today_tom_todayPlus2_dager = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_todayPlus2_dager" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusDays(2))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_today = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_today" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(LocalDate.now())
            .build();

        OrganiseringState organisering_fom_today_tom_todayMinus1 = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_todayMinus1" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();

        OrganiseringState organisering_fom_todayMinus10_tom_todayPlus10 = OrganiseringState.builder()
            .fid("organisering_fom_todayMinus10_tom_todayPlus10" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(LocalDate.now().plusDays(10))
            .build();

        OrganiseringState organisering_fom_today_tom_notset = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_notset" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now())
            .build();

        OrganiseringState organisering_fom_today_tom_empty = OrganiseringState.builder()
            .fid("organisering_fom_today_tom_empty" + "_fid")
            .id("the_id")
            .idOver("id_over")
            .gyldigFom(LocalDate.now())
            .gyldigTom(null)
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
    @DisplayName("Id ikke sett før i id-felt gir ny key")
    void should_add_new_key_from_id_if_not_present() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        OrganiseringState idInId = OrganiseringState.builder()
            .fid("1")
            .id(idToFind)
            .idOver("a_random_id")
            .build();
        repository.update(idInId);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(1);
        assertThat(organiseringerReturned).containsAll(List.of(idInId));
    }

    @Test
    @DisplayName("Id ikke sett før i idOver-felt gir ny key")
    void should_add_new_key_from_idOver_if_not_present() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        OrganiseringState idInIdOver = OrganiseringState.builder()
            .fid("1")
            .id("a_random_id")
            .idOver(idToFind)
            .build();
        repository.update(idInIdOver);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(1);
        assertThat(organiseringerReturned).containsAll(List.of(idInIdOver));
    }

    @Test
    @DisplayName("Id i idOver-felt som allerede finnes som key legger til i liste for key")
    void should_add_to_list_of_existing_key_for_idOver() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        OrganiseringState idInId = OrganiseringState.builder()
            .fid("1")
            .id(idToFind)
            .idOver("a_random_id")
            .build();
        repository.update(idInId);

        OrganiseringState idInIdOver = OrganiseringState.builder()
            .fid("2")
            .id("some_random_id")
            .idOver(idToFind)
            .build();
        repository.update(idInIdOver);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(2);
        assertThat(organiseringerReturned).containsAll(List.of(idInId, idInIdOver));
    }

    @Test
    @DisplayName("Id i id-felt som allerede er opprettet som key fra idOver legger til i liste for key")
    void should_add_to_list_of_existing_key_for_id() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        // ID mottas først i et idOver-felt
        OrganiseringState idInIdOver = OrganiseringState.builder()
            .fid("1")
            .id("some_random_id")
            .idOver(idToFind)
            .build();
        repository.update(idInIdOver);

        OrganiseringState idInId = OrganiseringState.builder()
            .fid("2")
            .id(idToFind)
            .idOver("a_random_id")
            .build();
        repository.update(idInId);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(2);
        assertThat(organiseringerReturned).containsAll(List.of(idInId, idInIdOver));
    }

    @Test
    @DisplayName("Id i id-felt som allerede finnes som key fra id-felt oppretter ikke ny key")
    void should_not_create_new_key_for_id_already_in_existing_key() {
        final String idToFind = "the_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        // ID mottas først en gang i id-felt
        OrganiseringState idToFindInId = OrganiseringState.builder()
            .fid("1")
            .id(idToFind)
            .idOver("a_random_id")
            .build();
        repository.update(idToFindInId);

        OrganiseringState alsoIdToFindInId = OrganiseringState.builder()
            .fid("2")
            .id(idToFind)
            .idOver("another_random_id")
            .build();
        repository.update(alsoIdToFindInId);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(1);
        assertThat(organiseringerReturned).containsAll(List.of(alsoIdToFindInId));
    }

    @Test
    @DisplayName("Identisk organisering legges ikke til i listen")
    void should_not_add_identical_organisering() {
        final String idOver = "id_over";
        final String orgenhetId = "orgenhet_id";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        OrganiseringState idToFindInId = OrganiseringState.builder()
            .fid("1")
            .id(orgenhetId)
            .idOver(idOver)
            .build();
        repository.update(idToFindInId);

        // Identisk som første organisering, bare fid er forskjellig
        OrganiseringState alsoIdToFindInId = OrganiseringState.builder()
            .fid("2")
            .id(orgenhetId)
            .idOver(idOver)
            .build();
        repository.update(alsoIdToFindInId);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idOver);

        assertThat(organiseringerReturned.size()).isEqualTo(1);
    }


    @Test
    @DisplayName("Alle organiseringer som inneholder samme id kommer i listen")
    void putOrUpdateAlleOrganiseringer_should_add_organisering_with_id_already_present() {
        final String idToFind = "the_id";
        final String differentId1 = "different_id_1";
        final String differentId2 = "different_id_2";
        OrganiseringStateRepository repository = new OrganiseringStateRepositoryImpl();

        OrganiseringState idInId = OrganiseringState.builder()
            .fid("1")
            .id(idToFind)
            .idOver("a_random_id")
            .build();
        repository.update(idInId);

        OrganiseringState idInIdOver1 = OrganiseringState.builder()
            .fid("2")
            .id("second_random_id")
            .idOver(idToFind)
            .build();
        repository.update(idInIdOver1);

        OrganiseringState idInIdOver2 = OrganiseringState.builder()
            .fid("3")
            .id("third_random_id")
            .idOver(idToFind)
            .build();
        repository.update(idInIdOver2);

        // En annen orgEnhet som ikke inneholder samme id
        OrganiseringState differentOrg = OrganiseringState.builder()
            .fid("4")
            .id(differentId1)
            .idOver(differentId2)
            .build();
        repository.update(differentOrg);

        List<OrganiseringState> organiseringerReturned = repository.getOrganiseringerContainingId(idToFind);

        assertThat(organiseringerReturned.size()).isEqualTo(3);
        assertThat(organiseringerReturned).containsAll(List.of(idInId, idInIdOver1, idInIdOver2));
        assertThat(organiseringerReturned).doesNotContain(differentOrg);
    }

}
