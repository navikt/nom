package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Stream;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursSluttdatoOppdatertEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursSluttetEventV1;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DelayedEventRepositoryTest extends TestBase {

    @BeforeEach
    void clearRepositories() {
        delayedEventRepository.deleteAll();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("should_deserialize_event_input")
    @DisplayName("Should save and deserialize events of type")
    void shouldDeserializeEvent(Event event, Class<Event> expectedType) {
        var savedDelayedEvent = delayedEventRepository.save(
            DelayedEvent.builder()
                .delayedUntil(LocalDate.now())
                .event(event)
                .build()
        );

        assertThat(delayedEventRepository.findById(savedDelayedEvent.getDelayedEventId()).orElseThrow().getEvent())
            .isInstanceOf(expectedType);
    }

    private static Stream<Arguments> should_deserialize_event_input() {
        return Stream.of(
            Arguments.of(orgtilknytningDagligOppfolgingEndretEventV1(), OrgtilknytningDagligOppfolgingEndretEventV1.class),
            Arguments.of(ressursSluttdatoOppdatertEventV1(LocalDate.now()), RessursSluttdatoOppdatertEventV1.class),
            Arguments.of(ressursSluttetEventV1(LocalDate.now()), RessursSluttetEventV1.class)
        );
    }

    @Test
    void test_findByDelayedUntilBefore() {

        var today = LocalDate.now();
        var yesterday = today.minusDays(1);
        var tomorrow = today.plusDays(1);

        delayedEventRepository.saveAll(
            Stream.of(today, yesterday, tomorrow).map(delayedUntil ->
                DelayedEvent.builder()
                    .delayedUntil(delayedUntil)
                    .event(ressursSluttdatoOppdatertEventV1(delayedUntil))
                    .build()
            ).toList()
        );

        assertThat(delayedEventRepository.findAll()).hasSize(3);

        assertThat(delayedEventRepository.findByDelayedUntilLessThanEqual(today))
            .hasSize(2)
            .allMatch(delayedEvent -> delayedEvent.getDelayedUntil().isBefore(today) || delayedEvent.getDelayedUntil().isEqual(today));
    }
}
