package no.nav.nom.apps.api.authorize.redigereressurs;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.redigereressurs.api.app.LederTilgangForOrgenhetRequest;
import no.nav.nom.apps.api.clients.msgraph.EntraIdUser;
import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AuthorizedNavident.class)
@TestPropertySource(
    properties = {
        "app.clients.microsoft-graph.groups.nom-delegert=42d11885-1e29-4fc4-9eed-1b339c2c8e36",
        "app.clients.microsoft-graph.groups.nom-leder=e9cd9267-9baf-4078-abb8-7251c4313d32",
        "app.orgenhet.helfo=dummyHelfoId",
        "app.orgenhet.skatt=dummySkattId",
        "app.orgenhet.riksrev=dummyRiksrevisjonenId"
    }
)
class AuthorizedNavidentTest extends MockBasisTestOrganisering {



    @MockitoBean
    AuthorizeCommandService authorizeCommandService;

    @MockitoBean
    DelegertLederStateRepository delegertLederStateRepository;

    @Mock
    Unleash unleash;

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

        assertTrue(kanRedigereB110001, "Leder A har auth til å redigere ressurs B110001");
        assertTrue(kanRedigereB110002, "Leder A har auth til å redigere ressurs B110002");
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

    private LederTilgangForOrgenhetRequest getDefaultRequest() {
        return LederTilgangForOrgenhetRequest.builder().orgenhetId("UKJENT").delegertTilNavident("HR").gyldigTom(LocalDate.now()).build();
    }

    @Test
    @DisplayName("Verifiser at Leder B ikke har auth til å delegere rettighet til ressurs HR for enhet D")
    void test_0009() {
        Exception exception = assertThrows(
            NotAuthorizedToDelegateLeder.class, () ->
                authorizedNavident.addLederTilgangForOrgenhet(
                    "B",
                    getDefaultRequest().toBuilder().orgenhetId("D").delegertTilNavident("HR").build()
                )
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for enhet som ikke finnes")
    void test_0010() {
        Exception exception = assertThrows(
            IllegalArgumentException.class, () ->
                authorizedNavident.addLederTilgangForOrgenhet(
                    "A",
                    getDefaultRequest().toBuilder().orgenhetId("UKJENT").delegertTilNavident("HR").build()
                )
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for enhet som er lavere enn arb.område")
    void test_0011() {
        Exception exception = assertThrows(
            IllegalArgumentException.class, () ->
                authorizedNavident.addLederTilgangForOrgenhet(
                    "A",
                    getDefaultRequest().toBuilder().orgenhetId("B1").delegertTilNavident("HR").build()
                )
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for ressurs som ikke er aktiv")
    void test_0012() {
        Exception exception = assertThrows(
            IllegalArgumentException.class, () ->
                authorizedNavident.addLederTilgangForOrgenhet(
                    "D",
                    getDefaultRequest().toBuilder().delegertTilNavident("INGEN_ANSATT").build()
                )
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til seg selv")
    void test_0013() {
        Exception exception = assertThrows(
            IllegalArgumentException.class, () ->
                authorizedNavident.addLederTilgangForOrgenhet(
                    "D",
                    getDefaultRequest().toBuilder().delegertTilNavident("D").build()
                )
        );
    }

    @Test
    @DisplayName("Verifiser at man kan delegere rettighet til sin egen enhet")
    void test_0014() {
        authorizedNavident.sjekkParametereForAddLederTilgang("D", getDefaultRequest().toBuilder().orgenhetId("D").delegertTilNavident("HR").build());
    }

    @Test
    @DisplayName("Verifiser at man kan delegere rettighet til enhet under sin egen som er mellom Driftsenhet og Arbeidsomraade")
    void test_0015() {
        authorizedNavident.sjekkParametereForAddLederTilgang("A1", getDefaultRequest().toBuilder().orgenhetId("A11").delegertTilNavident("HR").build());
    }

    @Test
    @DisplayName("Verifiser at delegert leder HR har auth til å redigere ressurs D100001 men ikke E100001")
    void test_0016() {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        boolean kanRedigereD100001 = authorizedNavident.kanBrukerRedigereRessurs("HR", "D100001");
        boolean kanRedigereE100001 = authorizedNavident.kanBrukerRedigereRessurs("HR", "E100001");

        assertTrue(kanRedigereD100001, "Leder D har ikke auth til å redigere ressurs D100001");
        assertFalse(kanRedigereE100001, "Leder D har auth til å redigere ressurs E100001");
    }

    @Test
    @DisplayName("Verifiser at Leder B har auth til å opprette ressurs i orgenhet B, B1 og B11")
    void test_0017() {
        boolean kanOppretteForEnhetB = authorizedNavident.kanBrukerOppretteRessurs("B", "B");
        boolean kanOppretteForEnhetB1 = authorizedNavident.kanBrukerOppretteRessurs("B", "B1");
        boolean kanOppretteForEnhetB11 = authorizedNavident.kanBrukerOppretteRessurs("B", "B11");

        assertTrue(kanOppretteForEnhetB, "Leder B har ikke auth til å opprette ressurs i enhet B");
        assertTrue(kanOppretteForEnhetB1, "Leder B har ikke auth til å opprette ressurs i enhet B1");
        assertTrue(kanOppretteForEnhetB11, "Leder B har ikke auth til å opprette ressurs i enhet B11");
    }

    @Test
    @DisplayName("Verifiser at Leder C har auth til å opprette ressurs i orgenhet C, C1, C11, C2 og C22")
    void test_0018() {
        boolean kanOppretteForEnhetC = authorizedNavident.kanBrukerOppretteRessurs("C", "C");
        boolean kanOppretteForEnhetC1 = authorizedNavident.kanBrukerOppretteRessurs("C", "C1");
        boolean kanOppretteForEnhetC11 = authorizedNavident.kanBrukerOppretteRessurs("C", "C11");
        boolean kanOppretteForEnhetC2 = authorizedNavident.kanBrukerOppretteRessurs("C", "C2");
        boolean kanOppretteForEnhetC22 = authorizedNavident.kanBrukerOppretteRessurs("C", "C22");

        assertTrue(kanOppretteForEnhetC, "Leder C har ikke auth til å opprette ressurs i enhet C");
        assertTrue(kanOppretteForEnhetC1, "Leder C har ikke auth til å opprette ressurs i enhet C1");
        assertTrue(kanOppretteForEnhetC11, "Leder C har ikke auth til å opprette ressurs i enhet C11");
        assertTrue(kanOppretteForEnhetC2, "Leder C har ikke auth til å opprette ressurs i enhet C2");
        assertTrue(kanOppretteForEnhetC22, "Leder C har ikke auth til å opprette ressurs i enhet C22");
    }

    @Test
    @DisplayName("Verifiser at Leder C3 har auth til å opprette ressurs i orgenhet C11 men ikke i C1 og C22")
    void test_0019() {
        boolean kanOppretteForEnhetC11 = authorizedNavident.kanBrukerOppretteRessurs("C3", "C11");
        boolean kanOppretteForEnhetC1 = authorizedNavident.kanBrukerOppretteRessurs("C3", "C1");
        boolean kanOppretteForEnhetC22 = authorizedNavident.kanBrukerOppretteRessurs("C3", "C22");

        assertTrue(kanOppretteForEnhetC11, "Leder C har ikke auth til å opprette ressurs i enhet C11");
        assertFalse(kanOppretteForEnhetC1, "Leder C har auth til å opprette ressurs i enhet C1");
        assertFalse(kanOppretteForEnhetC22, "Leder C har auth til å opprette ressurs i enhet C22");
    }

    @Test
    @DisplayName("Verifiser at Leder A har auth til å opprette ressurs i orgenhet B11, C1 og C22")
    void test_0020() {
        boolean kanOppretteForEnhetB11 = authorizedNavident.kanBrukerOppretteRessurs("A", "B11");
        boolean kanOppretteForEnhetC1 = authorizedNavident.kanBrukerOppretteRessurs("A", "C1");
        boolean kanOppretteForEnhetC22 = authorizedNavident.kanBrukerOppretteRessurs("A", "C22");

        assertTrue(kanOppretteForEnhetB11, "Leder A har auth til å opprette ressurs i enhet B11");
        assertTrue(kanOppretteForEnhetC1, "Leder A har auth til å opprette ressurs i enhet C1");
        assertTrue(kanOppretteForEnhetC22, "Leder A har auth til å opprette ressurs i enhet C22");
    }

    @Test
    @DisplayName("Verifiser at delegert leder HR har auth til å opprette ressurs i D1 men ikke i E1")
    void test_0021() {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        boolean kanOppretteForEnhetD1 = authorizedNavident.kanBrukerOppretteRessurs("HR", "D1");
        boolean kanOppretteForEnhetE1 = authorizedNavident.kanBrukerRedigereRessurs("HR", "E1");

        assertTrue(kanOppretteForEnhetD1, "Delegert leder HR har ikke auth til å opprette ressurs i enhet D1");
        assertFalse(kanOppretteForEnhetE1, "Delegert leder HR auth til å opprette ressurs i enhet E1");
    }

    @Test
    @DisplayName("Verifiser at leder har auth til å delegere tilgang til en annen bruker")
    void test_0022() {
        var kanDelegereForEnhetA = authorizedNavident.isAuthorizedToDelegate("A");
        assertTrue(!kanDelegereForEnhetA.isEmpty() && kanDelegereForEnhetA.getFirst().getId().equals("A"));
    }

    @Test
    @DisplayName("Verifiser at leder ikke har auth til å delegere tilgang til en annen bruker")
    void test_0023() {
        when(orgenhetService.isOrgEnhetInArbeidsomraade("B1")).thenReturn(true);
        var kanDelegereForEnhetB = authorizedNavident.isAuthorizedToDelegate("B1");
        assertTrue(kanDelegereForEnhetB.isEmpty());
    }

    @Test
    @DisplayName("Verifiser at Leder C får redigering til orgenhet C, C1, C11, C2 og C22")
    void test_0030() {
        var enheterLederKanRedigere = authorizedNavident.orgenheterLederKanRedigere("C");
        assertTrue(enheterLederKanRedigere.stream().anyMatch(orgenhet -> orgenhet.getId().equals("C")));
        assertTrue(enheterLederKanRedigere.stream().anyMatch(orgenhet -> orgenhet.getId().equals("C1")));
        assertTrue(enheterLederKanRedigere.stream().anyMatch(orgenhet -> orgenhet.getId().equals("C11")));
        assertTrue(enheterLederKanRedigere.stream().anyMatch(orgenhet -> orgenhet.getId().equals("C2")));
        assertTrue(enheterLederKanRedigere.stream().anyMatch(orgenhet -> orgenhet.getId().equals("C22")));
    }

    @Test
    @DisplayName("Verifiser at Leder H1 har auth til å redigere ressurser H100001")
    void test_0031() {
        boolean kanRedigereH100001 = authorizedNavident.kanBrukerRedigereRessurs("H1", "H100001");

        assertTrue(kanRedigereH100001, "Leder H1 har auth til å redigere ressurs H100001");
    }

    @Test
    @DisplayName("Verifiser at Leder A1 har auth til å redigere ressurser H100001")
    void test_0032() {
        boolean kanRedigereH100001 = authorizedNavident.kanBrukerRedigereRessurs("A1", "H100001");

        assertFalse(kanRedigereH100001, "Leder A1 har ikke auth til å redigere ressurs H100001");
    }

    @Test
    @DisplayName("kan hente hvilke ledere som kan redigere gitte orgenheter")
    void test_0033() {

        Function<String,RessursState> createRessurs = (navident) -> RessursState.builder().navident(navident).visningsnavn(new Visningsnavn(navident,Optional.of(navident),navident)).build();

        when(unleash.isEnabled(anyString(),anyBoolean())).thenReturn(true);
        when(ressursStateRepository.findByNavident("C")).thenReturn(Optional.of(createRessurs.apply("C")));
        when(ressursStateRepository.findByNavident("C2")).thenReturn(Optional.of(createRessurs.apply("C2")));
        when(ressursStateRepository.findByNavident("C3")).thenReturn(Optional.of(createRessurs.apply("C3")));
        when(ressursStateRepository.findByNavident("delegertleder")).thenReturn(Optional.of(createRessurs.apply("delegertleder")));
        when(delegertLederStateRepository.getDelegertLederByEnhet("C22")).thenReturn(List.of(new LederTilgangState("N/A","delegertleder","N/A","N/A",null,null)));

        var enheterLederKanRedigere = authorizedNavident.ledereSomKanRedigereOrgenhet(Set.of("C11","C1","C2","C22","C"));
        assertThat(enheterLederKanRedigere.size()).isEqualTo(5);
        assertThat(enheterLederKanRedigere.get("C").size()).isEqualTo(1);
        assertThat(enheterLederKanRedigere.get("C1").size()).isEqualTo(2);
        assertThat(enheterLederKanRedigere.get("C11").size()).isEqualTo(3);
        assertThat(enheterLederKanRedigere.get("C22").size()).isEqualTo(4); // sjekker også om delegert leder blir med

        // todo, hvis filtrering på nom-nivå blir tatt i bruk må dette testes også

    }

    @Test
    @DisplayName("Verifiser at man kan hente ut de øverste enhetene Leder C får redigering til (kun orgenhet C)")
    void test_0034() {
        var enheterLederKanRedigere = authorizedNavident.orgenheterLederKanRedigere("C",true);
        assertThat(enheterLederKanRedigere).hasSize(1);
        assertThat(enheterLederKanRedigere.getFirst().getId()).isNotNull().isEqualTo("C");
    }
}