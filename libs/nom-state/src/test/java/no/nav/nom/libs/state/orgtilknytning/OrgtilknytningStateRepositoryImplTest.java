package no.nav.nom.libs.state.orgtilknytning;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository.DOLIs;

class OrgtilknytningStateRepositoryImplTest {
    OrgtilknytningStateRepository orgtilknytningStateRepository = new OrgtilknytningStateRepositoryImpl();

    @Test
    void getAktiveNavidenterPaaEnhet() {
        var navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_1", DOLIs.TRUE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_1", DOLIs.FALSE);
        assertEquals(0, navidenter.size());

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_2", DOLIs.TRUE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_2"));

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_2", DOLIs.FALSE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_3", DOLIs.TRUE);
        assertEquals(0, navidenter.size());

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_3", DOLIs.FALSE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_2"));

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_2", DOLIs.ANY);
        assertEquals(2, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));
        assertTrue(navidenter.contains("NAVIDENT_2"));

        navidenter = orgtilknytningStateRepository.getAktiveNavidenterPaaEnhet("ENHET_2");
        assertEquals(2, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));
        assertTrue(navidenter.contains("NAVIDENT_2"));
    }

    @Test
    void getAktiveEllerFremtidigeNavidenterPaaEnhet() {
        var navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_1", DOLIs.TRUE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_1", DOLIs.FALSE);
        assertEquals(0, navidenter.size());

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_2", DOLIs.TRUE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_2"));

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_2", DOLIs.FALSE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_3", DOLIs.TRUE);
        assertEquals(0, navidenter.size());

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_3", DOLIs.FALSE);
        assertEquals(1, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_2"));

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_2", DOLIs.ANY);
        assertEquals(2, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));
        assertTrue(navidenter.contains("NAVIDENT_2"));

        navidenter = orgtilknytningStateRepository.getAktiveEllerFremtidigeNavidenterPaaEnhet("ENHET_2");
        assertEquals(2, navidenter.size());
        assertTrue(navidenter.contains("NAVIDENT_1"));
        assertTrue(navidenter.contains("NAVIDENT_2"));
    }

    @Test
    void getOrgtilknytningerPaaEnhet() {
        var OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_1", DOLIs.TRUE);
        assertEquals(3, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_1", DOLIs.FALSE);
        assertEquals(0, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_1", DOLIs.ANY);
        assertEquals(3, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_1");
        assertEquals(3, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_2", DOLIs.TRUE);
        assertEquals(2, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_2", DOLIs.FALSE);
        assertEquals(1, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_2", DOLIs.ANY);
        assertEquals(3, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_2");
        assertEquals(3, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_3", DOLIs.TRUE);
        assertEquals(0, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_3", DOLIs.FALSE);
        assertEquals(1, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_3", DOLIs.ANY);
        assertEquals(1, OTer.size());

        OTer = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("ENHET_3");
        assertEquals(1, OTer.size());
    }


    @BeforeEach
    void setUp() {
        orgtilknytningStateRepository.update(navident1_statlig_DOL_historisk_enhet1.fid(), navident1_statlig_DOL_historisk_enhet1);
        orgtilknytningStateRepository.update(navident1_statlig_DOL_Aktiv_enhet1.fid(), navident1_statlig_DOL_Aktiv_enhet1);
        orgtilknytningStateRepository.update(navident1_statlig_DOL_fremtidig_enhet1.fid(), navident1_statlig_DOL_fremtidig_enhet1);
        orgtilknytningStateRepository.update(navident1_statlig_ikke_DOL_Aktiv_enhet2.fid(), navident1_statlig_ikke_DOL_Aktiv_enhet2);
        orgtilknytningStateRepository.update(navident2_statlig_DOL_Aktiv_enhet2.fid(), navident2_statlig_DOL_Aktiv_enhet2);
        orgtilknytningStateRepository.update(navident2_statlig_DOL_fremtidig_enhet2.fid(), navident2_statlig_DOL_fremtidig_enhet2);
        orgtilknytningStateRepository.update(navident2_statlig_ikke_DOL_fremtidig_enhet3.fid(), navident2_statlig_ikke_DOL_fremtidig_enhet3);
    }

    // NAVIDENT_1
    OrgtilknytningState navident1_statlig_DOL_historisk_enhet1 = new OrgtilknytningState("FID_1", "NAVIDENT_1", "ENHET_1", Sektor.NAV_STATLIG, true, LocalDate.now().minusYears(3), Optional.of(LocalDate.now().minusYears(1).minusDays(1)), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");
    OrgtilknytningState navident1_statlig_DOL_Aktiv_enhet1 = new OrgtilknytningState("FID_2", "NAVIDENT_1", "ENHET_1", Sektor.NAV_STATLIG, true, LocalDate.now().minusYears(1), Optional.of(LocalDate.now().plusYears(1).minusDays(1)), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");
    OrgtilknytningState navident1_statlig_DOL_fremtidig_enhet1 = new OrgtilknytningState("FID_3", "NAVIDENT_1", "ENHET_1", Sektor.NAV_STATLIG, true, LocalDate.now().plusYears(1), Optional.empty(), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");

    OrgtilknytningState navident1_statlig_ikke_DOL_Aktiv_enhet2 = new OrgtilknytningState("FID_4", "NAVIDENT_1", "ENHET_2", Sektor.NAV_STATLIG, false, LocalDate.now().minusYears(1), Optional.empty(), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");

    // NAVIDENT_2
    OrgtilknytningState navident2_statlig_DOL_Aktiv_enhet2 = new OrgtilknytningState("FID_5", "NAVIDENT_2", "ENHET_2", Sektor.NAV_STATLIG, true, LocalDate.now().minusYears(1), Optional.of(LocalDate.now().plusYears(1).minusDays(1)), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");
    OrgtilknytningState navident2_statlig_DOL_fremtidig_enhet2 = new OrgtilknytningState("FID_6", "NAVIDENT_2", "ENHET_2", Sektor.NAV_STATLIG, true, LocalDate.now().plusYears(1), Optional.empty(), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");

    OrgtilknytningState navident2_statlig_ikke_DOL_fremtidig_enhet3 = new OrgtilknytningState("FID_7", "NAVIDENT_2", "ENHET_3", Sektor.NAV_STATLIG, false, LocalDate.now().minusYears(1), Optional.empty(), Optional.of(Tilknytningtype.FAST), Optional.empty(), "S123456");

}