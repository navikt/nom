package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Tester gruppering og periodisering av RessursStillinger fra DVH")
class RessursStillingLoaderTest extends TestBase {
    @Autowired
    private RessursStillingLoader ressursStillingLoader;

    @Autowired
    private Unleash unleash;

    @MockitoBean
    OrgenhetService orgenhetService;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    private static IOrgenhet agr_8620005, agr_8552106, agr_0807001;

    @BeforeAll
    public static void init() {
        agr_8620005 = new OrgEnhet(new OrgEnhetState(
            "FID_1",
            "x111yz",
            "Data og innsikt gruppe 5",
            LocalDate.of(2000, 1, 1),
            null,
            null,
            null,
            null,
            null,
            "8620005-ORGENHET-Data og innsikt gruppe 5",
            "8620005",
            "ORGENHET",
            null,
            null
        ));
        agr_8552106 = new OrgEnhet(new OrgEnhetState(
            "FID_2",
            "z222xy",
            "IT utvikling 6",
            LocalDate.of(2000, 1, 1),
            null,
            null,
            null,
            null,
            null,
            "8552106-ORGENHET-IT utvikling 6",
            "8552106",
            "ORGENHET",
            null,
            null
        ));
        agr_0807001 = new OrgEnhet(new OrgEnhetState(
            "FID_3",
            "z999xy",
            "Notodden lagene",
            LocalDate.of(2000, 1, 1),
            null,
            null,
            null,
            null,
            null,
            "0807001-ORGENHET-Notodden lagene",
            "0807001",
            "ORGENHET",
            null,
            null
        ));
    }

    @BeforeEach
    public void beforeEach() {
        when(ressursStateRepository.findByNavident("G123321"))
            .thenReturn(Optional.of(new RessursState(
                "FID_1",
                "01010154321",
                "G123321",
                Sektor.NAV_STATLIG,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                IdentType.STANDARD,
                null,
                null
            )));
        when(ressursStateRepository.findByNavident("H554433"))
            .thenReturn(Optional.of(new RessursState(
                "FID_2",
                "02020254321",
                "H554433",
                Sektor.NAV_STATLIG,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                IdentType.STANDARD,
                null,
                null
            )));
    }

    @Test
    @DisplayName("Test last av Resssurs stillinger fra Dvh json")
    public void testLoad() {
        var result = ressursStillingLoader.load();

        assertEquals(7, result.size());
        assertEquals(4, result.stream().filter(rs -> rs.getNavident().equals("A100036")).count());
        assertEquals(
            LocalDate.of(2021, 7, 1),
            result.stream()
                .filter(rs -> rs.getNavident().equals("A100036"))
                .min(Comparator.comparing(DvhRessursStilling::getStillingDatoFom))
                .get()
                .getStillingDatoFom()
        );
        assertEquals(
            LocalDate.of(2022, 1, 1),
            result.stream()
                .filter(rs -> rs.getNavident().equals("A100036"))
                .max(Comparator.comparing(DvhRessursStilling::getStillingDatoFom))
                .get()
                .getStillingDatoFom()
        );
        assertEquals(3, result.stream().filter(rs -> rs.getNavident().equals("A100180")).count());
        assertEquals(
            LocalDate.of(2024, 6, 1),
            result.stream()
                .filter(rs -> rs.getNavident().equals("A100180"))
                .min(Comparator.comparing(DvhRessursStilling::getStillingDatoFom))
                .get()
                .getStillingDatoFom()
        );
    }

    @Test
    @DisplayName("Test last av Ressurs stillinger fra Dvh json")
    public void testLoadToggleFalse() {
        var result = ressursStillingLoader.load();

        assertEquals(7, result.size());
        assertEquals(4, result.stream().filter(rs -> rs.getNavident().equals("A100036")).count());
        assertEquals(
            LocalDate.of(2021, 7, 1),
            result.stream()
                .filter(rs -> rs.getNavident().equals("A100036"))
                .min(Comparator.comparing(DvhRessursStilling::getStillingDatoFom))
                .get()
                .getStillingDatoFom()
        );
        assertEquals(
            LocalDate.of(2022, 1, 1),
            result.stream()
                .filter(rs -> rs.getNavident().equals("A100036"))
                .max(Comparator.comparing(DvhRessursStilling::getStillingDatoFom))
                .get()
                .getStillingDatoFom()
        );
        assertEquals(3, result.stream().filter(rs -> rs.getNavident().equals("A100180")).count());
        assertEquals(
            LocalDate.of(2024, 6, 1),
            result.stream()
                .filter(rs -> rs.getNavident().equals("A100180"))
                .min(Comparator.comparing(DvhRessursStilling::getStillingDatoFom))
                .get()
                .getStillingDatoFom()
        );
    }

    @Test
    @DisplayName("Test last og prossesering av Resssurs stillinger fra Dvh json")
    public void testloadAndProcess() {
        /* Se last innhold nederst i denne filen */
        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(agr_8620005, agr_8552106, agr_0807001));

        ressursStillingLoader.loadAndProcess();
    }

    private DvhRessursStilling getTestRessursStilling(
        String navid,
        String stillingAgressoId,
        String stillingOrgenhetNavn,
        String ressursOrgenhetNiv,
        LocalDate from,
        LocalDate to
    ) {
        return getTestRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, stillingAgressoId, ressursOrgenhetNiv, from, to);
    }

    private DvhRessursStilling getTestRessursStilling(
        String navid,
        String stillingAgressoId,
        String stillingOrgenhetNavn,
        String ressursAgressoId,
        String ressursOrgenhetNiv,
        LocalDate from,
        LocalDate to
    ) {
        return DvhRessursStilling.builder()
            .navident(navid)
            .stillingAgressoId(stillingAgressoId)
            .stillingOrgenhetNavn(stillingOrgenhetNavn)
            .ressursAgressoId(ressursAgressoId)
            .ressursOrgenhetNiv(ressursOrgenhetNiv)
            .stillingDatoFom(from)
            .stillingDatoTom(to)
            .build();
    }
}