package no.nav.nom.apps.api.dvh.dvhdiff;

import no.nav.nom.apps.api.DvhRessursUtils;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@DisplayName("dvh-diffing")
public class RessursEndringFraDvhTest extends DvhTestBase {

    @SpyBean
    private KafkaTemplate<String, Event> eventProducerTemplate;

    @Test
    @DisplayName("Ressurser fra datavarehus trigger opprett event")
    public void test1() {
        final var response = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        "Navn 1",
                        "ENavn 1",
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                ),
                new DvhRessursUtils.RessursMock(
                        "O123457",
                        "12345678902",
                        "Navn 2",
                        "ENavn 2",
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                )
        );
        dvhRessursUtils.addToResponse(response);

        ArgumentCaptor<RessursOpprettetEventV1> eventArgumentCaptor = ArgumentCaptor.forClass(RessursOpprettetEventV1.class);

        loadFromDvh();
        Mockito.verify(eventProducerTemplate, Mockito.times(response.size())).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());
        final var eventsByPersonident = eventArgumentCaptor.getAllValues()
                .stream()
                .collect(Collectors.toMap(e -> ((RessursOpprettetEventV1.RessursOpprettetEventData) e.getData()).personident(), Function.identity()));

        response.forEach(r -> assertOpprettetEventFromDvhRessurs(eventsByPersonident.get(r.FNR()), r));
    }

    @Test
    @DisplayName("Prøver ikke opprette hvis eksisterende Ressurs")
    public void test2() {
        RessursState existingRessurs = createRessursHendelse("");

        dvhRessursUtils.addToResponse(
                new DvhRessursUtils.RessursMock(
                        existingRessurs.navident(),
                        existingRessurs.personident(),
                        existingRessurs.visningsnavn().fornavn(),
                        existingRessurs.visningsnavn().etternavn(),
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                )
        );
        loadFromDvh();
        Mockito.verifyNoInteractions(eventProducerTemplate);
    }

    @Test
    @DisplayName("Sluttet ansatt trigger opprett og sluttet event")
    public void test3() {
        final var response = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        "Navn 1",
                        "ENavn 1",
                        "NAV Statlig",
                        LocalDate.parse("2021-12-31")
                )
        );
        dvhRessursUtils.addToResponse(response);

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

        loadFromDvh();
        Mockito.verify(eventProducerTemplate, Mockito.times(2)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());


        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    @Test
    @DisplayName("Ansatt som slutter i etterkant trigger sluttet event")
    public void test4() {
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

        final var response = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        "Navn 1",
                        "ENavn 1",
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                )
        );
        dvhRessursUtils.addToResponse(response);
        loadFromDvh();


        createRessursHendelse(
                new RessursState(
                        UUID.randomUUID(),
                        "12345678901",
                        "O123456",
                        Sektor.NAV_STATLIG,
                        new Visningsnavn(
                                "Navn 1",
                                Optional.empty(),
                                "ENavn 1"
                        ),
                        Optional.empty()
                )
        );

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        "Navn 1",
                        "ENavn 1",
                        "NAV Statlig",
                        LocalDate.now()
                )
        );
        dvhRessursUtils.addToResponse(response2);
        loadFromDvh();

        Mockito.verify(eventProducerTemplate, Mockito.times(2)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    private void assertOpprettetEventFromDvhRessurs(RessursOpprettetEventV1 ressursSluttetEvent, DvhRessursUtils.RessursMock ressursMock){

        Assertions.assertNotNull(ressursSluttetEvent.getFid());

        RessursOpprettetEventV1.RessursOpprettetEventData eventData = (RessursOpprettetEventV1.RessursOpprettetEventData) ressursSluttetEvent.getData();

        Assertions.assertEquals(ressursMock.NAV_IDENT(), ((RessursOpprettetEventV1.RessursOpprettetAdditionalData)ressursSluttetEvent.getAdditionalData()).navident().get());
        Assertions.assertEquals(ressursMock.FNR(), eventData.personident());
        Assertions.assertEquals(DvhRessurs.toSektor(ressursMock.ANSATT_TYPE()), eventData.sektor());
        Assertions.assertEquals(ressursMock.FORNAVN(), eventData.visningsnavn().fornavn());
        Assertions.assertEquals(ressursMock.ETTERNAVN(), eventData.visningsnavn().etternavn());
        Assertions.assertNull(eventData.visningsnavn().mellomnavn());
    }

}
