package no.nav.nom.libs.state.orgenhetleder;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrgEnhetLederStateRepositoryImplTest {
    OrgEnhetLederStateRepository orgEnhetLederStateRepository = new OrgEnhetLederStateRepositoryImpl();

    @Test
    void findAllFutureByOrgEnhetId_returnererKunLedereMedStartdatoEtterIDag() {
        var fremtidige = orgEnhetLederStateRepository.findAllFutureByOrgEnhetId("ENHET_1");

        assertEquals(2, fremtidige.size());
        assertTrue(fremtidige.stream().allMatch(leder -> leder.startdato().isAfter(LocalDate.now())));
        assertTrue(fremtidige.stream().noneMatch(leder -> leder.fid().equals("FID_AKTIV")));
        assertTrue(fremtidige.stream().noneMatch(leder -> leder.fid().equals("FID_HISTORISK")));
    }

    @Test
    void findAllFutureByOrgEnhetId_sortererStigendePaaStartdato() {
        var fremtidige = orgEnhetLederStateRepository.findAllFutureByOrgEnhetId("ENHET_1");

        assertEquals("FID_FREMTIDIG_1", fremtidige.getFirst().fid());
        assertEquals("FID_FREMTIDIG_2", fremtidige.getLast().fid());
    }

    @Test
    void findAllFutureByOrgEnhetId_returnererKunLedereForAngittEnhet() {
        var fremtidige = orgEnhetLederStateRepository.findAllFutureByOrgEnhetId("ENHET_2");

        assertEquals(1, fremtidige.size());
        assertEquals("FID_ANNEN_ENHET", fremtidige.getFirst().fid());
    }

    @Test
    void findAllFutureByOrgEnhetId_returnererTomListeForEnhetUtenFremtidigLeder() {
        assertTrue(orgEnhetLederStateRepository.findAllFutureByOrgEnhetId("UKJENT_ENHET").isEmpty());
    }

    @Test
    void findAllFutureByOrgEnhetId_ekskludererLederMedNullStartdato() {
        var fremtidige = orgEnhetLederStateRepository.findAllFutureByOrgEnhetId("ENHET_1");

        assertTrue(fremtidige.stream().noneMatch(leder -> leder.fid().equals("FID_NULL_START")));
    }

    @BeforeEach
    void setUp() {
        orgEnhetLederStateRepository.update(aktivLederEnhet1.fid(), aktivLederEnhet1);
        orgEnhetLederStateRepository.update(historiskLederEnhet1.fid(), historiskLederEnhet1);
        orgEnhetLederStateRepository.update(fremtidigLeder1Enhet1.fid(), fremtidigLeder1Enhet1);
        orgEnhetLederStateRepository.update(fremtidigLeder2Enhet1.fid(), fremtidigLeder2Enhet1);
        orgEnhetLederStateRepository.update(fremtidigLederEnhet2.fid(), fremtidigLederEnhet2);
        orgEnhetLederStateRepository.update(lederUtenStartdatoEnhet1.fid(), lederUtenStartdatoEnhet1);
    }

    OrgEnhetLederState aktivLederEnhet1 = OrgEnhetLederState.builder()
        .fid("FID_AKTIV")
        .navident("A000001")
        .orgEnhetId("ENHET_1")
        .startdato(LocalDate.now().minusYears(1))
        .build();

    OrgEnhetLederState historiskLederEnhet1 = OrgEnhetLederState.builder()
        .fid("FID_HISTORISK")
        .navident("A000002")
        .orgEnhetId("ENHET_1")
        .startdato(LocalDate.now().minusYears(3))
        .sluttdato(LocalDate.now().minusYears(1).minusDays(1))
        .build();

    OrgEnhetLederState fremtidigLeder1Enhet1 = OrgEnhetLederState.builder()
        .fid("FID_FREMTIDIG_1")
        .navident("A000003")
        .orgEnhetId("ENHET_1")
        .startdato(LocalDate.now().plusMonths(1))
        .build();

    OrgEnhetLederState fremtidigLeder2Enhet1 = OrgEnhetLederState.builder()
        .fid("FID_FREMTIDIG_2")
        .navident("A000004")
        .orgEnhetId("ENHET_1")
        .startdato(LocalDate.now().plusMonths(3))
        .build();

    OrgEnhetLederState fremtidigLederEnhet2 = OrgEnhetLederState.builder()
        .fid("FID_ANNEN_ENHET")
        .navident("A000005")
        .orgEnhetId("ENHET_2")
        .startdato(LocalDate.now().plusMonths(2))
        .build();

    OrgEnhetLederState lederUtenStartdatoEnhet1 = OrgEnhetLederState.builder()
        .fid("FID_NULL_START")
        .navident("A000006")
        .orgEnhetId("ENHET_1")
        .build();
}
