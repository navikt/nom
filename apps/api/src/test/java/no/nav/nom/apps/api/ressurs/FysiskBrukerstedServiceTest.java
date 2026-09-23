package no.nav.nom.apps.api.ressurs;

import freemarker.template.Configuration;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.speiling.SpeilingService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(FysiskBrukerstedService.class)
class FysiskBrukerstedServiceTest {
    @Autowired
    FysiskBrukerstedService fysiskBrukerstedService;

    @MockitoBean
    RessursStateRepository ressursStateRepositoryMock;
    @MockitoBean
    OrgtilknytningStateRepository orgtilknytningStateRepositoryMock;
    @MockitoBean
    RessursCommandService ressursCommandServiceMock;
    @MockitoBean
    FysiskBrukerstedCache fysiskBrukerstedCacheMock;
    @MockitoBean
    AdClient adClient;
    @MockitoBean
    Configuration freemarkerConfig;
    @MockitoBean
    MicrosoftGraphClient microsoftGraphClient;
    @MockitoBean
    OrgtilknytningCommandService orgtilknytningCommandServiceMock;
    @MockitoBean
    SpeilingService speilingServiceMock;
    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListenerMock;
    @MockitoBean
    Unleash unleash;


    @Test
    void getAlleRessurserMedFysiskBrukersted() {
        var fysiskBrukerstedToMatch = "1234";
        when(ressursStateRepositoryMock.getAll()).thenReturn(
            List.of(
                ressursState(),
                ressursState().toBuilder()
                    .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
                    .build(),
                ressursState(),
                ressursState().toBuilder()
                    .primaryFysiskBrukersted("DO NOT MATCH")
                    .build()
            )
        );

        var result  = fysiskBrukerstedService.getAlleRessurserMedFysiskBrukersted(fysiskBrukerstedToMatch);
        assertTrue(result.stream().allMatch(ressurs -> fysiskBrukerstedToMatch.equals(ressurs.primaryFysiskBrukersted())));
        assertEquals(1, result.size());
    }

    @Test
    void getAlleRessurserPaaEnhetMedFysiskBrukersted() {
        var navident1 = "A000001";
        var navident2 = "A000002";
        var navident3 = "A000003";

        var orgenhetId = "ab123c";
        var fysiskBrukerstedToMatch = "1234";

        var ressurs1 = ressursState().toBuilder()
            .navident(navident1)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs3 = ressursState().toBuilder()
            .navident(navident3)
            .primaryFysiskBrukersted("DO NOT MATCH")
            .build();

        when(orgtilknytningStateRepositoryMock.getAktiveEllerFremtidigeNavidenterPaaEnhet(orgenhetId)).thenReturn(
            List.of(navident1, navident2, navident3)
        );

        when(ressursStateRepositoryMock.findByNavidentBulk(List.of(navident1, navident2, navident3))).thenReturn(
            Map.of(navident1, ressurs1, navident2, ressurs2, navident3, ressurs3)
        );

        var result  = fysiskBrukerstedService.getAlleRessurserPaaEnhetMedFysiskBrukersted(orgenhetId, fysiskBrukerstedToMatch);
        assertTrue(result.stream().allMatch(ressurs -> fysiskBrukerstedToMatch.equals(ressurs.primaryFysiskBrukersted())));
        assertEquals(2, result.size());
    }

    @Test
    void flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var navident1 = "A000001";
        var navident2 = "A000002";

        var ressurs1 = ressursState().toBuilder()
            .navident(navident1)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(fysiskBrukerstedNotToMatch)
            .build();

        when(ressursStateRepositoryMock.getAll()).thenReturn(
            List.of(
                ressursState(),
                ressurs1,
                ressursState(),
                ressurs2
            )
        );
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr("1234").navn("Der").build(),
                FysiskBrukersted.builder().nr("9876").navn("Her").build()
            )
        );
        when(ressursStateRepositoryMock.findByNavident(navident1)).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(navident2)).thenReturn(Optional.of(ressurs2));

        fysiskBrukerstedService.flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(fysiskBrukerstedToMatch, "9876", null);

        verify(ressursCommandServiceMock, times(1)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted_illegalFysiskBrukersted() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var navident1 = "A000001";
        var navident2 = "A000002";

        var ressurs1 = ressursState().toBuilder()
            .navident(navident1)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(fysiskBrukerstedNotToMatch)
            .build();

        when(ressursStateRepositoryMock.getAll()).thenReturn(
            List.of(
                ressursState(),
                ressurs1,
                ressursState(),
                ressurs2
            )
        );
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Der").build()
            )
        );
        when(ressursStateRepositoryMock.findByNavident(navident1)).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(navident2)).thenReturn(Optional.of(ressurs2));

        var failed = fysiskBrukerstedService.flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(fysiskBrukerstedToMatch, "5555", null);

        assertEquals(1, failed.size());

        verify(ressursCommandServiceMock, times(0)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted_illegalFysiskBrukersted_2() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var navident1 = "A000001";
        var navident2 = "A000002";

        var ressurs1 = ressursState().toBuilder()
            .navident(navident1)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();

        when(ressursStateRepositoryMock.getAll()).thenReturn(
            List.of(
                ressursState(),
                ressurs1,
                ressursState(),
                ressurs2
            )
        );
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Der").build()
            )
        );
        when(ressursStateRepositoryMock.findByNavident(navident1)).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(navident2)).thenReturn(Optional.of(ressurs2));

        var failed = fysiskBrukerstedService.flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(fysiskBrukerstedToMatch, "5555", null);

        assertEquals(2, failed.size());

        verify(ressursCommandServiceMock, times(0)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttAlleRessurserPaaEnhetFraFysiskBrukerstedTilFysiskBrukersted() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var orgenhetToMatch = "ab123c";

        var navident1 = "A000001";  // Aktuell brukersted
        var navident2 = "A000002";  // Ikke aktuell brukersted
        var navident3 = "A000003";  // Aktuell brukersted
        var navident4 = "A000004";  // Aktuell brukersted - men feil orgenhet

        var ressurs1 = ressursState().toBuilder()
            .navident(navident1)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(fysiskBrukerstedNotToMatch)
            .build();
        var ressurs3 = ressursState().toBuilder()
            .navident(navident3)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();
        var ressurs4 = ressursState().toBuilder()
            .navident(navident4)
            .primaryFysiskBrukersted(fysiskBrukerstedToMatch)
            .build();

        when(orgtilknytningStateRepositoryMock.getAktiveNavidenterPaaEnhet(orgenhetToMatch)).thenReturn(
            List.of(navident1, navident2, navident3)
        );

        when(ressursStateRepositoryMock.findByNavidentBulk(List.of(navident1, navident2, navident3, navident4))).thenReturn(
            Map.of(navident1, ressurs1, navident2, ressurs2, navident3, ressurs3, navident4, ressurs4)
        );

        when(ressursStateRepositoryMock.getAll()).thenReturn(
            List.of(
                ressursState(),
                ressurs1,
                ressursState(),
                ressurs2
            )
        );
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Der").build()
            )
        );
        when(ressursStateRepositoryMock.findByNavident(navident1)).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(navident2)).thenReturn(Optional.of(ressurs2));

        var failed = fysiskBrukerstedService.flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(fysiskBrukerstedToMatch, "9876", null);
        assertEquals(0, failed.size());

        verify(ressursCommandServiceMock, times(1)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttRessurserTilFysiskBrukersted_ok() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var ressurs1 = ressursState().toBuilder().navident("A000001").build();
        var ressurs2 = ressursState().toBuilder().navident("A000002").build();

        when(ressursStateRepositoryMock.findByNavident(ressurs1.navident())).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(ressurs2.navident())).thenReturn(Optional.of(ressurs2));
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Der").build()
            )
        );

        fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(List.of(ressurs1.navident(), ressurs2.navident()), fysiskBrukerstedToMatch, null);

        verify(ressursCommandServiceMock, times(2)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttOrgtilknytningerTilNyFysiskBrukersted_ok() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var previousFysiskBrukersted = "4321";
        var orgtilknytning1 = orgtilknytningState().toBuilder().navident("A000001").primaryFysiskBrukerstedNummer(previousFysiskBrukersted).build();
        var orgtilknytning2 = orgtilknytningState().toBuilder().navident("A000002").primaryFysiskBrukerstedNummer(previousFysiskBrukersted).build();

        var allFids = List.of(orgtilknytning1.fid(), orgtilknytning2.fid());
        var allOts = List.of(orgtilknytning1, orgtilknytning2);

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Ikke der").build()
            )
        );

        fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, fysiskBrukerstedToMatch, previousFysiskBrukersted, null);

        verify(orgtilknytningCommandServiceMock, times(2)).endreFysiskeBrukersted(any());
    }

    @Test
    void flyttRessurserTilFysiskBrukersted_fails() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var ressurs1 = ressursState().toBuilder().navident("A000001").build();
        var ressurs2 = ressursState().toBuilder().navident("A000002").build();

        when(ressursStateRepositoryMock.findByNavident(ressurs1.navident())).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(ressurs2.navident())).thenReturn(Optional.of(ressurs2));
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Der").build()
            )
        );

        var failed = fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(List.of(ressurs1.navident(), ressurs2.navident()), "FINNES IKKE", null);
        assertEquals(2, failed.size());

        verify(ressursCommandServiceMock, times(0)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttOrgtilknytningerTilNyFysiskBrukersted_fails() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var fysiskBrukerstedOriginal = "4321";
        var orgtilknytning1 = orgtilknytningState().toBuilder().primaryFysiskBrukerstedNummer(fysiskBrukerstedOriginal).navident("A000001").build();
        var orgtilknytning2 = orgtilknytningState().toBuilder().primaryFysiskBrukerstedNummer(fysiskBrukerstedOriginal).navident("A000002").build();

        var allFids = List.of(orgtilknytning1.fid(), orgtilknytning2.fid());
        var allOts = List.of(orgtilknytning1, orgtilknytning2);

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Ikke der").build()
            )
        );

        var failed = fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, "FINNES IKKE", fysiskBrukerstedOriginal, null);
        assertEquals(2, failed.size());

        verify(orgtilknytningCommandServiceMock, times(0)).endreFysiskeBrukersted(any());
    }

    @Test
    void flyttRessurserTilFysiskBrukersted_fails_2() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var ressurs1 = ressursState().toBuilder().navident("A000001").build();
        var ressurs2 = ressursState().toBuilder().navident("A000002").build();

        when(ressursStateRepositoryMock.findByNavident(ressurs1.navident())).thenReturn(Optional.of(ressurs1));
        when(ressursStateRepositoryMock.findByNavident(ressurs2.navident())).thenReturn(Optional.of(ressurs2));
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Der").build()
            )
        );
        var failed = fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(List.of("FINNES IKKE", ressurs2.navident()), fysiskBrukerstedToMatch, null);
        assertEquals(1, failed.size());

        verify(ressursCommandServiceMock, times(1)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void flyttOrgtilknytningerTilNyFysiskBrukersted_fails_2() {
        var fysiskBrukerstedToMatch = "1234";
        var fysiskBrukerstedNotToMatch = "9876";
        var fysiskBrukerstedOriginal = "4321";
        var orgtilknytning1 = orgtilknytningState().toBuilder().fid("FINNES_IKKE").navident("A000001").build();
        var orgtilknytning2 = orgtilknytningState().toBuilder().primaryFysiskBrukerstedNummer(fysiskBrukerstedOriginal).navident("A000002").build();

        var allFids = List.of(orgtilknytning1.fid(), orgtilknytning2.fid());
        var allOts = List.of(orgtilknytning2);

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build(),
                FysiskBrukersted.builder().nr(fysiskBrukerstedNotToMatch).navn("Ikke der").build()
            )
        );

        var failed = fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, fysiskBrukerstedToMatch,fysiskBrukerstedOriginal , null);
        assertEquals(1, failed.size());

        verify(orgtilknytningCommandServiceMock, times(1)).endreFysiskeBrukersted(any());
    }

    @Test
    void getAlleRessurserMedFysiskBrukerstedIAktivOrgtilknytning() {
        var fysiskBrukerstedToMatch = "1234";
        var navidentPrimary = "A000001";
        var navidentAdditional = "A000002";
        var navidentInactive = "A000003";
        var navidentFuture = "A000004";
        var navidentNoMatch = "A000005";

        var ressursPrimary = ressursState().toBuilder().navident(navidentPrimary).build();
        var ressursAdditional = ressursState().toBuilder().navident(navidentAdditional).build();

        var today = LocalDate.now();

        var aktivPrimaryMatch = OrgtilknytningState.builder()
            .fid(UUID.randomUUID().toString())
            .navident(navidentPrimary)
            .orgEnhetId("oe1")
            .gyldigFom(today.minusDays(5))
            .gyldigTom(today.plusDays(5))
            .primaryFysiskBrukerstedNummer(fysiskBrukerstedToMatch)
            .additionalFysiskeBrukerstederNummer(List.of())
            .build();

        var aktivAdditionalMatch = OrgtilknytningState.builder()
            .fid(UUID.randomUUID().toString())
            .navident(navidentAdditional)
            .orgEnhetId("oe1")
            .gyldigFom(today.minusDays(5))
            .gyldigTom(null)
            .primaryFysiskBrukerstedNummer("9999")
            .additionalFysiskeBrukerstederNummer(List.of("1111", fysiskBrukerstedToMatch))
            .build();

        var inaktivMatch = OrgtilknytningState.builder()
            .fid(UUID.randomUUID().toString())
            .navident(navidentInactive)
            .orgEnhetId("oe1")
            .gyldigFom(today.minusDays(30))
            .gyldigTom(today.minusDays(10))
            .primaryFysiskBrukerstedNummer(fysiskBrukerstedToMatch)
            .build();

        var fremtidigMatch = OrgtilknytningState.builder()
            .fid(UUID.randomUUID().toString())
            .navident(navidentFuture)
            .orgEnhetId("oe1")
            .gyldigFom(today.plusDays(5))
            .gyldigTom(today.plusDays(10))
            .primaryFysiskBrukerstedNummer(fysiskBrukerstedToMatch)
            .build();

        var aktivNoMatch = OrgtilknytningState.builder()
            .fid(UUID.randomUUID().toString())
            .navident(navidentNoMatch)
            .orgEnhetId("oe1")
            .gyldigFom(today.minusDays(5))
            .gyldigTom(null)
            .primaryFysiskBrukerstedNummer("9999")
            .additionalFysiskeBrukerstederNummer(List.of("1111", "2222"))
            .build();

        when(orgtilknytningStateRepositoryMock.getAll()).thenReturn(
            List.of(aktivPrimaryMatch, aktivAdditionalMatch, inaktivMatch, fremtidigMatch, aktivNoMatch)
        );

        when(ressursStateRepositoryMock.findByNavidentBulk(any())).thenReturn(
            Map.of(navidentPrimary, ressursPrimary, navidentAdditional, ressursAdditional)
        );

        var result = fysiskBrukerstedService.getAlleRessurserMedFysiskBrukerstedIAktivOrgtilknytning(fysiskBrukerstedToMatch);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.navident().equals(navidentPrimary)));
        assertTrue(result.stream().anyMatch(r -> r.navident().equals(navidentAdditional)));
    }

    @Test
    void fysiskBrukerstedExists() {
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr("1234").navn("Der").build(),
                FysiskBrukersted.builder().nr("9876").navn("Her").build()
            )
        );

        assertTrue(fysiskBrukerstedService.fysiskBrukerstedExists("1234"));
        assertTrue(fysiskBrukerstedService.fysiskBrukerstedExists("9876"));
        assertFalse(fysiskBrukerstedService.fysiskBrukerstedExists("5555"));
    }

    @Test
    void verifyUnrelatedOtToPreviousFysiskBrukerstedNummerIsIgnored(){

        var fysiskBrukerstedToMatch = "1234";
        var previousFysiskBrukersted = "4321";
        var unrelatedPreviousFysiskBrukersted = "4001";
        var orgtilknytning1 = orgtilknytningState().toBuilder().navident("A000001").primaryFysiskBrukerstedNummer(previousFysiskBrukersted).build();
        var orgtilknytning2 = orgtilknytningState().toBuilder().navident("A000002").primaryFysiskBrukerstedNummer(previousFysiskBrukersted).build();
        var unrlatedOrgtilknytning3 = orgtilknytningState().toBuilder().navident("A000003").primaryFysiskBrukerstedNummer(unrelatedPreviousFysiskBrukersted).build();

        var allFids = List.of(orgtilknytning1.fid(), orgtilknytning2.fid(), unrlatedOrgtilknytning3.fid());
        var allOts = List.of(orgtilknytning1, orgtilknytning2, unrlatedOrgtilknytning3);

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fysiskBrukerstedToMatch).navn("Der").build()
            )
        );

        fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, fysiskBrukerstedToMatch, previousFysiskBrukersted, null);

        verify(orgtilknytningCommandServiceMock, times(2)).endreFysiskeBrukersted(any());

    }

    @Test
    void changesAdditionalBrukerstedWhenMatching(){
        var newBrukersted = "1234";
        var previousMatchingAdditionalFysiskBrukersted = "4321";
        var previousNonMatchingPrimaryFysiskBrukersted = "9876";
        var orgtilknytning1 = orgtilknytningState().toBuilder().navident("A000001").primaryFysiskBrukerstedNummer(previousNonMatchingPrimaryFysiskBrukersted).additionalFysiskeBrukerstederNummer(List.of(previousMatchingAdditionalFysiskBrukersted)).build();

        var allFids = List.of(orgtilknytning1.fid());
        var allOts = List.of(orgtilknytning1);

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(newBrukersted).navn("Der").build()
            )
        );

        fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, newBrukersted, previousMatchingAdditionalFysiskBrukersted, null);

        verify(orgtilknytningCommandServiceMock, times(1)).endreFysiskeBrukersted(argThat(s -> {
            assertTrue(s.additionalFysiskeBrukerstederNummer().contains(newBrukersted),"should contain new fysisk brukersted");
            assertEquals(previousNonMatchingPrimaryFysiskBrukersted, s.primaryFysiskBrukerstedNummer(), "Should not change non-matching primarybrukersted");
            return true;
        }));
    }

    @Test
    void leavesUnrelatedAdditionalBrukerstedIntact(){
        var newBrukersted = "1234";
        var previousMatchingAdditionalFysiskBrukersted = "4321";
        var previousNonMatchingAdditionalFysiskBrukersted = "9876";
        var orgtilknytning1 = orgtilknytningState().toBuilder()
            .navident("A000001")
            .additionalFysiskeBrukerstederNummer(List.of(previousMatchingAdditionalFysiskBrukersted,previousNonMatchingAdditionalFysiskBrukersted))
            .build();

        var allFids = List.of(orgtilknytning1.fid());
        var allOts = List.of(orgtilknytning1);

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(newBrukersted).navn("Der").build()
            )
        );

        fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, newBrukersted, previousMatchingAdditionalFysiskBrukersted, null);

        verify(orgtilknytningCommandServiceMock, times(1)).endreFysiskeBrukersted(argThat(s -> {
            assertTrue(s.additionalFysiskeBrukerstederNummer().contains(newBrukersted),"should contain new fysisk brukersted");
            assertTrue(s.additionalFysiskeBrukerstederNummer().contains(previousNonMatchingAdditionalFysiskBrukersted),"should not change non-matching primarybrukersted");
            return true;
        }));
    }

    @Test
    void checkOverallMasseflyttBehavior(){
        var fbrA = "1234";
        var fbrB = "4321";
        var fbrC = "4001";
        var fbrD = "5004";
        var ot1 = orgtilknytningState().toBuilder().navident("A000001").primaryFysiskBrukerstedNummer(fbrA).additionalFysiskeBrukerstederNummer(List.of(fbrB)).build();
        var ot2 = orgtilknytningState().toBuilder().navident("A000002").primaryFysiskBrukerstedNummer(fbrB).additionalFysiskeBrukerstederNummer(List.of(fbrA)).build();
        var ot3 = orgtilknytningState().toBuilder().navident("A000003").primaryFysiskBrukerstedNummer(fbrC).additionalFysiskeBrukerstederNummer(List.of(fbrA)).build();
        var ot4 = orgtilknytningState().toBuilder().navident("A000004").primaryFysiskBrukerstedNummer(fbrC).additionalFysiskeBrukerstederNummer(List.of(fbrA, fbrB, fbrD)).build();
        var otSkip1 = orgtilknytningState().toBuilder().navident("B000001").primaryFysiskBrukerstedNummer(fbrC).build();
        var otSkip2 = orgtilknytningState().toBuilder().navident("B000002").primaryFysiskBrukerstedNummer(fbrC).additionalFysiskeBrukerstederNummer(List.of(fbrB)).build();
        var otSkip3 = orgtilknytningState().toBuilder().navident("B000003").primaryFysiskBrukerstedNummer(fbrB).build();
        var otSkip4 = orgtilknytningState().toBuilder().navident("B000004").primaryFysiskBrukerstedNummer(fbrB).additionalFysiskeBrukerstederNummer(List.of(fbrD)).build();

        var otForChangeList = List.of(ot1, ot2, ot3, ot4);
        var otForSkipList = List.of(otSkip1, otSkip2, otSkip3, otSkip4);

        var allOts = Stream.concat(otForChangeList.stream(), otForSkipList.stream()).toList();
        var allFids = allOts.stream().map(OrgtilknytningState::fid).toList();

        when(orgtilknytningStateRepositoryMock.findByFidIn(allFids)).thenReturn(allOts);
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                FysiskBrukersted.builder().nr(fbrA).navn("fbr a").build(),
                FysiskBrukersted.builder().nr(fbrB).navn("fbr b").build(),
                FysiskBrukersted.builder().nr(fbrC).navn("fbr c").build(),
                FysiskBrukersted.builder().nr(fbrD).navn("fbr d").build()
            )
        );
        var failures = fysiskBrukerstedService.flyttOrgtilknytningerTilNyFysiskBrukersted(allFids, fbrB, fbrA, null);

        assertThat(failures).hasSize(otForSkipList.size()).containsOnlyKeys(otForSkipList.stream().map(OrgtilknytningState::fid).toList());
        final var changeCommandMapFromNavident = new HashMap<String, EndreFysiskeBrukerstederCommand>();

        verify(orgtilknytningCommandServiceMock, times(4)).endreFysiskeBrukersted(argThat(s -> {
            changeCommandMapFromNavident.put(s.navident(),s);
            return true;
        }));
        { // masseflytt A til B: [A,[B]] -> [B,[]]
            var change1 = changeCommandMapFromNavident.get(ot1.navident());
            assertThat(change1.primaryFysiskBrukerstedNummer()).isEqualTo(fbrB);
            assertThat(change1.additionalFysiskeBrukerstederNummer()).isEmpty();
        }
        { // masseflytt A til B: [B,[A]] -> [B,[]]
            var change2 = changeCommandMapFromNavident.get(ot2.navident());
            assertThat(change2.primaryFysiskBrukerstedNummer()).isEqualTo(fbrB);
            assertThat(change2.additionalFysiskeBrukerstederNummer()).isEmpty();
        }
        {  // masseflytt A til B: [C,[A]] -> [C,[B]]
            var change3 = changeCommandMapFromNavident.get(ot3.navident());
            assertThat(change3.primaryFysiskBrukerstedNummer()).isEqualTo(fbrC);
            assertThat(change3.additionalFysiskeBrukerstederNummer()).containsExactly(fbrB);
        }
        { // masseflytt A til B: [C,[A,B,D]] -> [C,[B,D]]
            var change4 = changeCommandMapFromNavident.get(ot4.navident());
            assertThat(change4.primaryFysiskBrukerstedNummer()).isEqualTo(fbrC);
            assertThat(change4.additionalFysiskeBrukerstederNummer()).containsExactlyInAnyOrder(fbrB,fbrD);
        }
        {
            for (var ot : List.of(otSkip1, otSkip2, otSkip3, otSkip4)) {
                assertNull(changeCommandMapFromNavident.get(ot.navident()), "Expected this OT connected to navident " + ot.navident() + " to not have been changed");
            }
        }
    }


    public static RessursState ressursState() {
        return new RessursState(
            UUID.randomUUID().toString(),
            "11111111111",
            "X123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn("Nav", Optional.empty(), "Navesen"),
            null,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            "S123456",
            null
        );
    }
}