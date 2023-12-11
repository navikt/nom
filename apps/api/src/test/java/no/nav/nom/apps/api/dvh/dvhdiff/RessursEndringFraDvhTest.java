package no.nav.nom.apps.api.dvh.dvhdiff;

import no.nav.nom.apps.api.DvhRessursUtils;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("dvh-diffing")
public class RessursEndringFraDvhTest extends DvhTestBase {

    @SpyBean
    private KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;
    @MockBean
    NomMastered nomMastered;

    @BeforeEach
    public void setup() {
        // Default mock svar på nomMastered ressurs og Unleash flag
        when(nomMastered.isMastered(any())).thenReturn(false);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(false);

        Mockito.reset(ressursEventProducerTemplate);
    }

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
        Mockito.verify(ressursEventProducerTemplate, Mockito.times(response.size())).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());
        final var eventsByPersonident = eventArgumentCaptor.getAllValues()
                .stream()
                .collect(Collectors.toMap(e -> ((RessursOpprettetEventV1.RessursOpprettetEventData) e.getData()).personident(), Function.identity()));

        response.forEach(r -> assertOpprettetEventFromDvhRessurs(eventsByPersonident.get(r.FNR()), r));
    }

    @Test
    @DisplayName("Prøver ikke opprette hvis eksisterende Ressurs")
    public void test2() {
        RessursState existingRessurs = createRessursHendelseEventSource("");

        dvhRessursUtils.addToResponse(
                new DvhRessursUtils.RessursMock(
                        existingRessurs.navident(),
                        existingRessurs.personident(),
                        existingRessurs.visningsnavn().getFornavnMellomnavn(),
                        existingRessurs.visningsnavn().etternavn(),
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                )
        );
        loadFromDvh();
        Mockito.verifyNoInteractions(ressursEventProducerTemplate);
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

        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

        loadFromDvh();
        Mockito.verify(ressursEventProducerTemplate, Mockito.times(2)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());


        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    @Test
    @DisplayName("Ansatt som slutter i etterkant trigger sluttet event")
    public void test4() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        createRessursHendelseEventSource(
                new RessursState(
                        PrefixedUlidCreator.createRessursUlid(),
                        "12345678901",
                        "O123456",
                        Sektor.NAV_STATLIG,
                        new Visningsnavn(
                                "Navn 1",
                                Optional.empty(),
                                "ENavn 1"
                        ),
                        Optional.empty(),
                        Optional.empty(),
                    null,
                    Collections.emptyList()
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

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    @Test
    @DisplayName("Ansatt som endrer fornavn PesonopplysningerEndretEvent")
    public void test5() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        final var ressursv1 = new RessursState(
                PrefixedUlidCreator.createRessursUlid(),
                "12345678901",
                "O123456",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "Navn 1",
                        Optional.empty(),
                        "ENavn 1"
                ),
                Optional.empty(),
                Optional.empty(),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursv1);

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        "Navn 1 ENDRET",
                        ressursv1.visningsnavn().etternavn(),
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                )
        );
        dvhRessursUtils.addToResponse(response2);
        loadFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursPersonaliaOppdatertV1));
    }

    @Test
    @DisplayName("Ansatt som endrer etternavn PesonopplysningerEndretEvent")
    public void test6() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        final var ressursv1 = new RessursState(
                PrefixedUlidCreator.createRessursUlid(),
                "12345678901",
                "O123456",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "Navn 1",
                        Optional.empty(),
                        "ENavn 1"
                ),
                Optional.empty(),
                Optional.empty(),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursv1);

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        ressursv1.visningsnavn().fornavn(),
                        "ENavn 1 ENDRET",
                        "NAV Statlig",
                        LocalDate.parse("2099-12-31")
                )
        );
        dvhRessursUtils.addToResponse(response2);
        loadFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursPersonaliaOppdatertV1));
    }

    /*************************************************************************
     * Tester for NomMastered og ulike settinger av unleash flag
     ************************************************************************/

     @Test
     @DisplayName("Ansatt som slutter i etterkant trigger sluttet event - når NomMasterd unleash flag er satt og navident er markert")
     public void test_104a() {
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

         //Unleash flagg satt og ident er mastret
         when(nomMastered.isMastered(any())).thenReturn(true);
         when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

         loadFromDvh();
         Mockito.verifyNoInteractions(ressursEventProducerTemplate);
     }

    @Test
    @DisplayName("Ansatt som slutter i etterkant trigger sluttet event - når NomMasterd unleash flag ikke er satt og navident er markert som mastret")
    public void test_104b() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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

        //Unleash flagg ikke satt og ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(false);

        loadFromDvh();

        createRessursHendelseEventSource(
            new RessursState(
                PrefixedUlidCreator.createRessursUlid(),
                "12345678901",
                "O123456",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                    "Navn 1",
                    Optional.empty(),
                    "ENavn 1"
                ),
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList()
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

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    @Test
    @DisplayName("Ansatt som slutter i etterkant trigger sluttet event - når NomMasterd unleash flag er satt og navident ikke er markert som mastret")
    public void test_104c() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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

        //Unleash flagg satt og ident ikke er mastret
        when(nomMastered.isMastered(any())).thenReturn(false);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        loadFromDvh();

        createRessursHendelseEventSource(
            new RessursState(
                PrefixedUlidCreator.createRessursUlid(),
                "12345678901",
                "O123456",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                    "Navn 1",
                    Optional.empty(),
                    "ENavn 1"
                ),
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList()
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

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    @Test
    @DisplayName("Ansatt som endrer fornavn PesonopplysningerEndretEvent - når NomMasterd unleash flag er satt og navident er markert som mastret")
    public void test_105a() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        final var ressursv1 = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursv1);

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
            new DvhRessursUtils.RessursMock(
                "O123456",
                "12345678901",
                "Navn 1 ENDRET",
                ressursv1.visningsnavn().etternavn(),
                "NAV Statlig",
                LocalDate.parse("2099-12-31")
            )
        );
        dvhRessursUtils.addToResponse(response2);

        //Unleash flagg satt og ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        Mockito.reset(ressursEventProducerTemplate);

        loadFromDvh();
        Mockito.verifyNoInteractions(ressursEventProducerTemplate);
    }

    @Test
    @DisplayName("Ansatt som endrer fornavn PesonopplysningerEndretEvent - når NomMasterd unleash flag ikke er satt og navident er markert som mastret")
    public void test_105b() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        final var ressursv1 = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursv1);

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
            new DvhRessursUtils.RessursMock(
                "O123456",
                "12345678901",
                "Navn 1 ENDRET",
                ressursv1.visningsnavn().etternavn(),
                "NAV Statlig",
                LocalDate.parse("2099-12-31")
            )
        );
        dvhRessursUtils.addToResponse(response2);

        //Unleash flagg ikke satt og ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(false);

        loadFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursPersonaliaOppdatertV1));
    }

    @Test
    @DisplayName("Ansatt som endrer fornavn PesonopplysningerEndretEvent - når NomMasterd unleash flag er satt og navident ikke er markert som mastret")
    public void test_105c() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        final var ressursv1 = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursv1);

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
            new DvhRessursUtils.RessursMock(
                "O123456",
                "12345678901",
                "Navn 1 ENDRET",
                ressursv1.visningsnavn().etternavn(),
                "NAV Statlig",
                LocalDate.parse("2099-12-31")
            )
        );
        dvhRessursUtils.addToResponse(response2);

        //Unleash flagg satt og ident ikke er mastret
        when(nomMastered.isMastered(any())).thenReturn(false);
        when(unleash.isEnabled(NomMastered.UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS)).thenReturn(true);

        loadFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursPersonaliaOppdatertV1));
    }


    private void assertOpprettetEventFromDvhRessurs(RessursOpprettetEventV1 ressursSluttetEvent, DvhRessursUtils.RessursMock ressursMock) {

        Assertions.assertNotNull(ressursSluttetEvent.getFid());

        RessursOpprettetEventV1.RessursOpprettetEventData eventData = (RessursOpprettetEventV1.RessursOpprettetEventData) ressursSluttetEvent.getData();

        Assertions.assertEquals(
                ressursMock.NAV_IDENT(),
                ((RessursOpprettetEventV1.RessursOpprettetAdditionalData) ressursSluttetEvent.getAdditionalData()).navident().get()
        );
        Assertions.assertEquals(ressursMock.FNR(), eventData.personident());
        Assertions.assertEquals(DvhRessurs.toSektor(ressursMock.ANSATT_TYPE()), eventData.sektor());
        Assertions.assertEquals(ressursMock.FORNAVN(), eventData.visningsnavn().fornavn());
        Assertions.assertEquals(ressursMock.ETTERNAVN(), eventData.visningsnavn().etternavn());
        Assertions.assertTrue(eventData.visningsnavn().mellomnavn().isEmpty());
    }

    @Test
    @DisplayName("Ansatt som endrer sektor RessursSektorOppdatertEvent")
    public void test7() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

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


        final var ressursv1 = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            Optional.empty(),
            Optional.empty(),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursv1);

        dvhRessursUtils.createEmptyResponse();

        final var response2 = List.of(
            new DvhRessursUtils.RessursMock(
                "O123456",
                "12345678901",
                ressursv1.visningsnavn().fornavn(),
                ressursv1.visningsnavn().etternavn(),
                "NAV Kommunalt",
                LocalDate.parse("2099-12-31")
            )
        );
        dvhRessursUtils.addToResponse(response2);
        loadFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(3)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSektorOppdatertV1));
    }


}
