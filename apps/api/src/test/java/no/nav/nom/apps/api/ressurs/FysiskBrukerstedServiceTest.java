package no.nav.nom.apps.api.ressurs;

import freemarker.template.Configuration;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    Unleash unleash;


    @Test
    void getAlleRessurserMedFysiskBrukersted() {
        var fysiskBrukerstedToMatch = "1234";
        when(ressursStateRepositoryMock.getAll()).thenReturn(
            List.of(
                ressursState(),
                ressursState().toBuilder()
                    .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
                    .build(),
                ressursState(),
                ressursState().toBuilder()
                    .primaryFysiskBrukersted(Optional.of("DO NOT MATCH"))
                    .build()
            )
        );

        var result  = fysiskBrukerstedService.getAlleRessurserMedFysiskBrukersted(fysiskBrukerstedToMatch);
        assertTrue(result.stream().allMatch(ressurs -> fysiskBrukerstedToMatch.equals(ressurs.primaryFysiskBrukersted().orElse(null))));
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
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs3 = ressursState().toBuilder()
            .navident(navident3)
            .primaryFysiskBrukersted(Optional.of("DO NOT MATCH"))
            .build();

        when(orgtilknytningStateRepositoryMock.getAktiveEllerFremtidigeNavidenterPaaEnhet(orgenhetId)).thenReturn(
            List.of(navident1, navident2, navident3)
        );

        when(ressursStateRepositoryMock.findByNavidentBulk(List.of(navident1, navident2, navident3))).thenReturn(
            Map.of(navident1, ressurs1, navident2, ressurs2, navident3, ressurs3)
        );

        var result  = fysiskBrukerstedService.getAlleRessurserPaaEnhetMedFysiskBrukersted(orgenhetId, fysiskBrukerstedToMatch);
        assertTrue(result.stream().allMatch(ressurs -> fysiskBrukerstedToMatch.equals(ressurs.primaryFysiskBrukersted().orElse(null))));
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
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedNotToMatch))
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
                new FysiskBrukersted("1234", "Der"),
                new FysiskBrukersted("9876", "Her")
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
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedNotToMatch))
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
                new FysiskBrukersted(fysiskBrukerstedToMatch, "Der"),
                new FysiskBrukersted(fysiskBrukerstedNotToMatch, "Der")
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
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
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
                new FysiskBrukersted(fysiskBrukerstedToMatch, "Der"),
                new FysiskBrukersted(fysiskBrukerstedNotToMatch, "Der")
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
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs2 = ressursState().toBuilder()
            .navident(navident2)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedNotToMatch))
            .build();
        var ressurs3 = ressursState().toBuilder()
            .navident(navident3)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
            .build();
        var ressurs4 = ressursState().toBuilder()
            .navident(navident4)
            .primaryFysiskBrukersted(Optional.of(fysiskBrukerstedToMatch))
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
                new FysiskBrukersted(fysiskBrukerstedToMatch, "Der"),
                new FysiskBrukersted(fysiskBrukerstedNotToMatch, "Der")
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
                new FysiskBrukersted(fysiskBrukerstedToMatch, "Der"),
                new FysiskBrukersted(fysiskBrukerstedNotToMatch, "Der")
            )
        );

        fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(List.of(ressurs1.navident(), ressurs2.navident()), fysiskBrukerstedToMatch, null);

        verify(ressursCommandServiceMock, times(2)).oppdaterFysiskeBrukerstederCommand(any(), any());
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
                new FysiskBrukersted(fysiskBrukerstedToMatch, "Der"),
                new FysiskBrukersted(fysiskBrukerstedNotToMatch, "Der")
            )
        );

        var failed = fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(List.of(ressurs1.navident(), ressurs2.navident()), "FINNES IKKE", null);
        assertEquals(2, failed.size());

        verify(ressursCommandServiceMock, times(0)).oppdaterFysiskeBrukerstederCommand(any(), any());
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
                new FysiskBrukersted(fysiskBrukerstedToMatch, "Der"),
                new FysiskBrukersted(fysiskBrukerstedNotToMatch, "Der")
            )
        );
        var failed = fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(List.of("FINNES IKKE", ressurs2.navident()), fysiskBrukerstedToMatch, null);
        assertEquals(1, failed.size());

        verify(ressursCommandServiceMock, times(1)).oppdaterFysiskeBrukerstederCommand(any(), any());
    }

    @Test
    void fysiskBrukerstedExists() {
        when(fysiskBrukerstedCacheMock.getAll()).thenReturn(
            List.of(
                new FysiskBrukersted("1234", "Der"),
                new FysiskBrukersted("9876", "Her")
            )
        );

        assertTrue(fysiskBrukerstedService.fysiskBrukerstedExists("1234"));
        assertTrue(fysiskBrukerstedService.fysiskBrukerstedExists("9876"));
        assertFalse(fysiskBrukerstedService.fysiskBrukerstedExists("5555"));
    }

    public static RessursState ressursState() {
        return new RessursState(
            UUID.randomUUID().toString(),
            "11111111111",
            "X123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn("Nav", Optional.empty(), "Navesen"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Collections.emptyList(),
            IdentType.STANDARD
        );
    }
}