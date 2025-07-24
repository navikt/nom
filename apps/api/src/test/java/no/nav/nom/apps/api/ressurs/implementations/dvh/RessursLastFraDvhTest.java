package no.nav.nom.apps.api.ressurs.implementations.dvh;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.contract.nom.IdentType;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Last DvhRessurser og behandling av disse")
public class RessursLastFraDvhTest extends DvhTestBase {
    @MockitoSpyBean
    RessursCommandService ressursCommandService;
    @MockitoSpyBean
    KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;

    @BeforeEach
    public void setup() {
        when(nomMastered.isMastered(any())).thenReturn(false);
    }

    @Test
    @DisplayName("Ressurser fra datavarehus trigger opprett event")
    public void test1() {
        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build(),
            DvhRessurs.builder()
                .navIdent("O123457")
                .personIdent("12345678902")
                .fornavn("Navn 2")
                .etternavn("ENavn 2")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        ArgumentCaptor<OpprettRessursCommand> opprettRessursArgumentCaptor = ArgumentCaptor.forClass(OpprettRessursCommand.class);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verify(ressursCommandService, Mockito.times(dvhRessurser.size())).createRessursEvent(opprettRessursArgumentCaptor.capture());


        final var opprettRessursCommands = opprettRessursArgumentCaptor.getAllValues()
            .stream()
            .collect(Collectors.toMap(OpprettRessursCommand::personident, Function.identity()));

        dvhRessurser.forEach(r -> assertOpprettetEventFromDvhRessurs(opprettRessursCommands.get(r.getPersonIdent()), r));
    }

    @Test
    @DisplayName("Prøver ikke opprette hvis eksisterende Ressurs")
    public void test2() {
        RessursState existingRessurs = createRessursState("");

        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent(existingRessurs.navident())
                .personIdent(existingRessurs.personident())
                .fornavn(existingRessurs.visningsnavn().getFornavnMellomnavn())
                .etternavn(existingRessurs.visningsnavn().etternavn())
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(ressursStateRepository.findByNavident(existingRessurs.navident())).thenReturn(Optional.of(existingRessurs));
        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verifyNoInteractions(ressursCommandService);
    }

    @Test
    @DisplayName("Sluttet ansatt trigger opprett og sluttet event")
    public void test3() {
        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2021, 12, 31))
                .build()
        );

        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        ArgumentCaptor<OpprettRessursCommand> opprettRessursArgumentCaptor = ArgumentCaptor.forClass(OpprettRessursCommand.class);

        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verify(ressursCommandService, Mockito.times(dvhRessurser.size())).createRessursEvent(opprettRessursArgumentCaptor.capture());
        Mockito.verify(ressursCommandService, Mockito.times(dvhRessurser.size())).avsluttRessursEvent(any(), any());

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(2)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());


        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));

        final var opprettRessursCommands = opprettRessursArgumentCaptor.getAllValues()
            .stream()
            .collect(Collectors.toMap(OpprettRessursCommand::personident, Function.identity()));

        dvhRessurser.forEach(r -> assertOpprettetEventFromDvhRessurs(opprettRessursCommands.get(r.getPersonIdent()), r));
    }


    @Test
    @DisplayName("Ansatt som slutter i etterkant trigger sluttet event")
    public void test4() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        dvhRestConsumer.loadRessurserFromDvh();

        RessursState existingRessurs = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );

        dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.now())
                .build()
        );

        when(ressursStateRepository.findByPersonident(existingRessurs.personident())).thenReturn(Optional.of(existingRessurs));
        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verify(ressursCommandService, Mockito.times(1)).createRessursEvent(any());
        Mockito.verify(ressursCommandService, Mockito.times(1)).avsluttRessursEvent(any(), any());

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(2)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursOpprettetEventV1));
        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSluttetEventV1));
    }

    @Test
    @DisplayName("Ansatt som endrer fornavn PesonopplysningerEndretEvent")
    public void test5() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

        RessursState existingRessurs = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );

        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1 Endret")
                .etternavn("ENavn 1")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(ressursStateRepository.findByPersonident(existingRessurs.personident())).thenReturn(Optional.of(existingRessurs));
        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(1)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursPersonaliaOppdatertV1));
    }

    @Test
    @DisplayName("Ansatt som endrer etternavn PesonopplysningerEndretEvent")
    public void test6() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

        RessursState existingRessurs = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );

        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1 ENDRET")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(ressursStateRepository.findByPersonident(existingRessurs.personident())).thenReturn(Optional.of(existingRessurs));
        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(1)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursPersonaliaOppdatertV1));
    }

    @Test
    @DisplayName("Ansatt som endrer sektor RessursSektorOppdatertEvent")
    public void test7() {
        ArgumentCaptor<RessursEvent> eventArgumentCaptor = ArgumentCaptor.forClass(RessursEvent.class);

        RessursState existingRessurs = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );

        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1")
                .remedyAnsattType("NAV Kommunalt")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(ressursStateRepository.findByPersonident(existingRessurs.personident())).thenReturn(Optional.of(existingRessurs));
        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verify(ressursEventProducerTemplate, Mockito.times(1)).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());

        Assertions.assertTrue(eventArgumentCaptor.getAllValues().stream().anyMatch(e -> e instanceof RessursSektorOppdatertV1));
    }


    /*************************************************************************
     * Tester for NomMastered og ulike settinger av unleash flag
     ************************************************************************/

     @Test
     @DisplayName("Ansatt som slutter i etterkant trigger sluttet event - når navident er markert som mastret")
     public void test_104a() {
         RessursState existingRessurs = new RessursState(
             PrefixedUlidCreator.createRessursUlid(),
             "12345678901",
             "O123456",
             Sektor.NAV_STATLIG,
             new Visningsnavn(
                 "Navn 1",
                 Optional.empty(),
                 "ENavn 1"
             ),
             null,
             null,
             null,
             null,
             null,
             Collections.emptyList(),
             IdentType.STANDARD,
             null
         );

         var dvhRessurser = List.of(
             DvhRessurs.builder()
                 .navIdent("O123456")
                 .personIdent("12345678901")
                 .fornavn("Navn 1")
                 .etternavn("ENavn 1")
                 .remedyAnsattType("NAV Statlig")
                 .remedySluttdato(LocalDate.of(2099, 12, 31))
                 .build()
         );

         when(ressursStateRepository.findByPersonident(existingRessurs.personident())).thenReturn(Optional.of(existingRessurs));
         when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);
         when(nomMastered.isMastered(any())).thenReturn(true);

         Mockito.reset(ressursEventProducerTemplate);
         dvhRestConsumer.loadRessurserFromDvh();

         Mockito.verifyNoInteractions(ressursEventProducerTemplate);
         Mockito.verifyNoInteractions(ressursCommandService);
     }

    @Test
    @DisplayName("Ansatt som endrer fornavn PesonopplysningerEndretEvent - når navident er markert som mastret")
    public void test_105a() {
        RessursState existingRessurs = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "12345678901",
            "O123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "Navn 1",
                Optional.empty(),
                "ENavn 1"
            ),
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );

        var dvhRessurser = List.of(
            DvhRessurs.builder()
                .navIdent("O123456")
                .personIdent("12345678901")
                .fornavn("Navn 1")
                .etternavn("ENavn 1 ENDRET")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.of(2099, 12, 31))
                .build()
        );

        when(ressursStateRepository.findByPersonident(existingRessurs.personident())).thenReturn(Optional.of(existingRessurs));
        when(dvhOrdsService.getRessurser()).thenReturn(dvhRessurser);

        // ident er mastret
        when(nomMastered.isMastered(any())).thenReturn(true);

        Mockito.reset(ressursEventProducerTemplate);

        dvhRestConsumer.loadRessurserFromDvh();

        Mockito.verifyNoInteractions(ressursEventProducerTemplate);
        Mockito.verifyNoInteractions(ressursCommandService);
    }

    public RessursState createRessursState(String suffix) {
        final var ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "testPersonIdent" + suffix,
            "testNavIdent" + suffix,
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn" + suffix,
                Optional.of("mellomnavn" + suffix),
                "etternavn" + suffix
            ),
            null,
            null,
            null,
            LocalDate.now(),
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );
        return ressursState;
    }

    private void assertOpprettetEventFromDvhRessurs(OpprettRessursCommand opprettRessursCommand, DvhRessurs dvhRessurs) {
        Assertions.assertEquals(dvhRessurs.getPersonIdent(), opprettRessursCommand.personident());
        Assertions.assertEquals(DvhRessurs.toSektor(dvhRessurs.getRemedyAnsattType()), opprettRessursCommand.sektor());
        Assertions.assertEquals(dvhRessurs.getFornavn(), opprettRessursCommand.visningsnavn().fornavn());
        Assertions.assertEquals(dvhRessurs.getEtternavn(), opprettRessursCommand.visningsnavn().etternavn());
        Assertions.assertTrue(opprettRessursCommand.visningsnavn().mellomnavn().isEmpty());
    }
}
