package no.nav.nom.apps.api.authorize.redigereressurs;

import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.state.DelegertLederStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AuthorizedNavident.class)
class AuthorizedNavidentTest extends MockBasisTestOrganisering {

    @MockBean
    AuthorizeCommandService authorizeCommandService;
    @MockBean
    DelegertLederStateRepository delegertLederStateRepository;

    @Test
    @DisplayName("Verifiser at Leder B har auth til å redigere ressurser B110001 og B110002")
    void test_0001() {
        boolean kanRedigereB110001 = authorizedNavident.kanBrukerRedigereRessurs("B", "B110001");
        boolean kanRedigereB110002 = authorizedNavident.kanBrukerRedigereRessurs("B", "B110002");

        assertTrue(kanRedigereB110001, "Leder B har ikke auth til å redigere ressurs B110001");
        assertTrue(kanRedigereB110002, "Leder B har ikke auth til å redigere ressurs B110002");
    }

    @Test
    @DisplayName("Verifiser at Leder A ikke har auth til å redigere ressurser B110001 og B110002")
    void test_0002() {
        boolean kanRedigereB110001 = authorizedNavident.kanBrukerRedigereRessurs("A", "B110001");
        boolean kanRedigereB110002 = authorizedNavident.kanBrukerRedigereRessurs("A", "B110002");

        assertFalse(kanRedigereB110001, "Leder A har auth til å redigere ressurs B110001");
        assertFalse(kanRedigereB110002, "Leder A har auth til å redigere ressurs B110002");
    }

    @Test
    @DisplayName("Verifiser at Leder B ikke har auth til å redigere ressurser C110001")
    void test_0003() {
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("B", "C110001");

        assertFalse(kanRedigereC110001, "Leder B har auth til å redigere ressurs C110001");
    }

    @Test
    @DisplayName("Verifiser at Leder C har auth til å redigere ressurser C110001 og C220001")
    void test_0004() {
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("C", "C110001");
        boolean kanRedigereC220001 = authorizedNavident.kanBrukerRedigereRessurs("C", "C220001");

        assertTrue(kanRedigereC110001, "Leder C har ikke auth til å redigere ressurs C110001");
        assertTrue(kanRedigereC220001, "Leder C har ikke auth til å redigere ressurs C220001");
    }

    @Test
    @DisplayName("Verifiser at Leder C3 har auth til å redigere ressurs C110001 men ikke C220001")
    void test_0005() {
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("C3", "C110001");
        boolean kanRedigereC220001 = authorizedNavident.kanBrukerRedigereRessurs("C3", "C220001");

        assertTrue(kanRedigereC110001, "Leder C3 har ikke auth til å redigere ressurs C110001");
        assertFalse(kanRedigereC220001, "Leder C3 har auth til å redigere ressurs C220001");
    }

    @Test
    @DisplayName("Verifiser at Leder C2 har auth til å redigere ressurs C220001 men ikke C110001")
    void test_0006() {
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("C2", "C110001");
        boolean kanRedigereC220001 = authorizedNavident.kanBrukerRedigereRessurs("C2", "C220001");

        assertFalse(kanRedigereC110001, "Leder C2 har auth til å redigere ressurs C110001");
        assertTrue(kanRedigereC220001, "Leder C2 har ikke auth til å redigere ressurs C220001");
    }

    @Test
    @DisplayName("Verifiser at Leder D har auth til å redigere ressurs D100001 og E100001")
    void test_0008() {
        boolean kanRedigereD100001 = authorizedNavident.kanBrukerRedigereRessurs("D", "D100001");
        boolean kanRedigereE100001 = authorizedNavident.kanBrukerRedigereRessurs("D", "E100001");

        assertTrue(kanRedigereD100001, "Leder D har ikke auth til å redigere ressurs D100001");
        assertTrue(kanRedigereE100001, "Leder D har ikke auth til å redigere ressurs E100001");
    }


    @Test
    @DisplayName("Verifiser at Leder A ikke har auth til å delegere rettighet til ressurs HR for enhet D1")
    void test_0009() {
        Exception exception = assertThrows(NotAuthorizedToDelegateLeder.class, () ->
            authorizedNavident.addDelegertLederRolleForOrgenhet("A", "HR", "D1")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for enhet som ikke finnes")
    void test_0010() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederRolleForOrgenhet("A", "HR", "UKJENT")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for enhet som ikke er arb.område eller lavere")
    void test_0011() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederRolleForOrgenhet("A", "HR", "A")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for ressurs som ikke er aktiv")
    void test_0012() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederRolleForOrgenhet("D", "INGEN_ANSATT", "D1")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til seg selv")
    void test_0013() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederRolleForOrgenhet("D", "D", "D1")
        );
    }

    @Test
    @DisplayName("Verifiser at man kan delegere rettighet til sin egen enhet")
    void test_0014() {
        boolean delegert = authorizedNavident.addDelegertLederRolleForOrgenhet("D", "HR", "D");

        assertTrue(delegert, "Leder D Kunne ikke delegere rettighet til ressurs HR for enhet D");
    }

    @Test
    @DisplayName("Verifiser at man kan delegere rettighet til enhet under sin egen")
    void test_0015() {
        boolean delegert = authorizedNavident.addDelegertLederRolleForOrgenhet("D", "HR", "D1");

        assertTrue(delegert, "Leder D Kunne ikke delegere rettighet til ressurs HR for enhet D1");
    }

    @Test
    @DisplayName("Verifiser at delegert leder HR har auth til å redigere ressurs D100001 men ikke E100001")
    void test_0016() {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        boolean kanRedigereD100001 = authorizedNavident.kanBrukerRedigereRessurs("HR", "D100001");
        boolean kanRedigereE100001 = authorizedNavident.kanBrukerRedigereRessurs("HR", "E100001");
        // Assert
        assertTrue(kanRedigereD100001, "Leder D har ikke auth til å redigere ressurs D100001");
        assertFalse(kanRedigereE100001, "Leder D har auth til å redigere ressurs E100001");
    }
}