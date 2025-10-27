package no.nav.nom.apps.api.hierarki.initals;

import com.google.common.collect.Streams;
import no.nav.nom.apps.api.hierarki.HierarkiService;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class InitHierarkiStandard implements InitHierarkiInterface {

    /*

        // todo, update tree structure to accurately reflect hierarchy
        A
        ├── B
        │   └── B1
        │       └── B11
        ├── C
        │   ├── C1
        │   │   └── C11
        │   └── C2
        │       └── C22
        ├── D
        │   └── D1
        └── E
            └── E1

        H (helfo)
        S (skatt)
        R (riksrev)
     */


    /*
            =======     enheter
     */

    public final String X_ENHET_NOMID = "ab000x";
    public final OrgEnhetState X_ENHET = OrgEnhetState.builder()
        .id(X_ENHET_NOMID)
        .fid(X_ENHET_NOMID)
        .navn("Enhet X")
        .agressoId("X")
        .agressoOrgNivaa("NAV")
        .nomNivaa(NomNivaa.LINJEENHET)
        .build();


    public final String A_ENHET_NOMID = "ab000a";
    public final OrgEnhetState A_ENHET = OrgEnhetState.builder()
        .id(A_ENHET_NOMID)
        .fid(A_ENHET_NOMID)
        .navn("Enhet A")
        .agressoId("A")
        .agressoOrgNivaa("NAV")
        .nomNivaa(NomNivaa.DRIFTSENHET)
        .build();

    public final String A1_ENHET_NOMID = "ab001a";
    public final OrgEnhetState A1_ENHET = OrgEnhetState.builder()
        .id(A1_ENHET_NOMID)
        .fid(A1_ENHET_NOMID)
        .navn("Enhet A1")
        .agressoId("A1")
        .agressoOrgNivaa("ORGNIV2")
        .nomNivaa(null)
        .build();

    public final String A11_ENHET_NOMID = "ab011a";
    public final OrgEnhetState A11_ENHET = OrgEnhetState.builder()
        .id(A11_ENHET_NOMID)
        .fid(A11_ENHET_NOMID)
        .navn("Enhet A11")
        .agressoId("A11")
        .agressoOrgNivaa("ORGNIV3")
        .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
        .build();

    public final String B_ENHET_NOMID = "ab000b";
    public final OrgEnhetState B_ENHET = OrgEnhetState.builder()
        .id(B_ENHET_NOMID)
        .fid(B_ENHET_NOMID)
        .navn("Enhet B")
        .agressoId("B")
        .agressoOrgNivaa("ORGNIV3")
        .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
        .build();

    public final String B1_ENHET_NOMID = "ab001b";
    public final OrgEnhetState B1_ENHET = OrgEnhetState.builder()
        .id(B1_ENHET_NOMID)
        .fid(B1_ENHET_NOMID)
        .navn("Enhet B1")
        .agressoId("B1")
        .agressoOrgNivaa("ORGNIV4")
        .nomNivaa(null)
        .build();

    public final String B11_ENHET_NOMID = "ab011b";
    public final OrgEnhetState B11_ENHET = OrgEnhetState.builder()
        .id(B11_ENHET_NOMID)
        .fid(B11_ENHET_NOMID)
        .navn("Enhet B11")
        .agressoId("B11")
        .agressoOrgNivaa("ORGNIV4")
        .nomNivaa(null)
        .build();

    public final String C_ENHET_NOMID = "ab000c";
    public final OrgEnhetState C_ENHET = OrgEnhetState.builder()
        .id(C_ENHET_NOMID)
        .fid(C_ENHET_NOMID)
        .navn("Enhet C")
        .agressoId("C")
        .agressoOrgNivaa("<XXX>")
        .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
        .build();

    public final String C1_ENHET_NOMID = "ab001c";
    public final OrgEnhetState C1_ENHET = OrgEnhetState.builder()
        .id(C1_ENHET_NOMID)
        .fid(C1_ENHET_NOMID)
        .navn("Enhet C1")
        .agressoId("C1")
        .agressoOrgNivaa("ORGNIV4")
        .nomNivaa(null)
        .build();

    public final String C11_ENHET_NOMID = "ab011c";
    public final OrgEnhetState C11_ENHET = OrgEnhetState.builder()
        .id(C11_ENHET_NOMID)
        .fid(C11_ENHET_NOMID)
        .navn("Enhet C11")
        .agressoId("C11")
        .agressoOrgNivaa("ORGENHET")
        .nomNivaa(null)
        .build();

    public final String C2_ENHET_NOMID = "ab002c";
    public final OrgEnhetState C2_ENHET = OrgEnhetState.builder()
        .id(C2_ENHET_NOMID)
        .fid(C2_ENHET_NOMID)
        .navn("Enhet C2")
        .agressoId("C2")
        .agressoOrgNivaa("ORGNIV4")
        .nomNivaa(null)
        .build();

    public final String C22_ENHET_NOMID = "ab022c";
    public final OrgEnhetState C22_ENHET = OrgEnhetState.builder()
        .id(C22_ENHET_NOMID)
        .fid(C22_ENHET_NOMID)
        .navn("Enhet C22")
        .agressoId("C22")
        .agressoOrgNivaa("ORGENHET")
        .nomNivaa(null)
        .build();

    public final String D_ENHET_NOMID = "ab000d";
    public final OrgEnhetState D_ENHET = OrgEnhetState.builder()
        .id(D_ENHET_NOMID)
        .fid(D_ENHET_NOMID)
        .navn("Enhet D")
        .agressoId("D")
        .agressoOrgNivaa("ORGNIV3")
        .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
        .build();

    public final String D1_ENHET_NOMID = "ab001d";
    public final OrgEnhetState D1_ENHET = OrgEnhetState.builder()
        .id(D1_ENHET_NOMID)
        .fid(D1_ENHET_NOMID)
        .navn("Enhet D1")
        .agressoId("D1")
        .agressoOrgNivaa("ORGENHET")
        .nomNivaa(null)
        .build();

    public final String E_ENHET_NOMID = "ab000e";
    public final OrgEnhetState E_ENHET = OrgEnhetState.builder()
        .id(E_ENHET_NOMID)
        .fid(E_ENHET_NOMID)
        .navn("Enhet E")
        .agressoId("E")
        .agressoOrgNivaa("ORGNIV3")
        .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
        .build();

    public final String E1_ENHET_NOMID = "ab001e";
    public final OrgEnhetState E1_ENHET = OrgEnhetState.builder()
        .id(E1_ENHET_NOMID)
        .fid(E1_ENHET_NOMID)
        .navn("Enhet E1")
        .agressoId("E1")
        .agressoOrgNivaa("ORGENHET")
        .nomNivaa(null)
        .build();

    public final String HELFO_ENHET_NOMID = "ha000x";
    public final OrgEnhetState HELFO_ENHET = OrgEnhetState.builder()
        .id(HELFO_ENHET_NOMID)
        .fid(HELFO_ENHET_NOMID)
        .navn("Enhet HELFO")
        .agressoId("HELFO")
        .agressoOrgNivaa("NAV")
        .nomNivaa(null)
        .build();

    public final String SKATT_ENHET_NOMID = "sa000x";
    public final OrgEnhetState SKATT_ENHET = OrgEnhetState.builder()
        .id(SKATT_ENHET_NOMID)
        .fid(SKATT_ENHET_NOMID)
        .navn("Enhet Skatt")
        .agressoId("SKATT")
        .agressoOrgNivaa("NAV")
        .nomNivaa(null)
        .build();

    public final String RIKSREV_ENHET_NOMID = "ra000x";
    public final OrgEnhetState RIKSREV_ENHET = OrgEnhetState.builder()
        .id(RIKSREV_ENHET_NOMID)
        .fid(RIKSREV_ENHET_NOMID)
        .navn("Enhet riksrev")
        .agressoId("RIKSREV")
        .agressoOrgNivaa("NAV")
        .nomNivaa(null)
        .build();

    /*
            =======     organiseringer
     */

    public final OrganiseringState organisering_A_X = OrganiseringState.builder()
        .fid("FID_A_X")
        .id(A_ENHET_NOMID)
        .navn(A_ENHET.navn())
        .idOver(X_ENHET_NOMID)
        .navnOver(X_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_A1_A = OrganiseringState.builder()
        .fid("FID_A1_A")
        .id(A1_ENHET.id())
        .navn(A1_ENHET.navn())
        .idOver(A_ENHET.id())
        .navnOver(A_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_A11_A1 = OrganiseringState.builder()
        .fid("FID_A11_A1")
        .id(A11_ENHET.id())
        .navn(A11_ENHET.navn())
        .idOver(A1_ENHET.id())
        .navnOver(A1_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();


    public final OrganiseringState organisering_B_A = OrganiseringState.builder()
        .fid("FID_B_A")
        .id(B_ENHET.id())
        .navn(B_ENHET.navn())
        .idOver(A_ENHET.id())
        .navnOver(A_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_B1_B = OrganiseringState.builder()
        .fid("FID_B1_B")
        .id(B1_ENHET.id())
        .navn(B1_ENHET.navn())
        .idOver(B_ENHET.id())
        .navnOver(B_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_B11_B1 = OrganiseringState.builder()
        .fid("FID_B11_B1")
        .id(B11_ENHET.id())
        .navn(B11_ENHET.navn())
        .idOver(B1_ENHET.id())
        .navnOver(B1_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_C_A = OrganiseringState.builder()
        .fid("FID_C_A")
        .id(C_ENHET.id())
        .navn(C_ENHET.navn())
        .idOver(A_ENHET.id())
        .navnOver(A_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_C1_C = OrganiseringState.builder()
        .fid("FID_C1_C")
        .id(C1_ENHET.id())
        .navn(C1_ENHET.navn())
        .idOver(C_ENHET.id())
        .navnOver(C_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_C11_C1 = OrganiseringState.builder()
        .fid("FID_C11_C1")
        .id(C11_ENHET.id())
        .navn(C11_ENHET.navn())
        .idOver(C1_ENHET.id())
        .navnOver(C1_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_C2_C = OrganiseringState.builder()
        .fid("FID_C2_C")
        .id(C2_ENHET.id())
        .navn(C2_ENHET.navn())
        .idOver(C_ENHET.id())
        .navnOver(C_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_C22_C2 = OrganiseringState.builder()
        .fid("FID_C22_C2")
        .id(C22_ENHET.id())
        .navn(C22_ENHET.navn())
        .idOver(C2_ENHET.id())
        .navnOver(C2_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_D_A = OrganiseringState.builder()
        .fid("FID_D_A")
        .id(D_ENHET.id())
        .navn(D_ENHET.navn())
        .idOver(A_ENHET.id())
        .navnOver(A_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_D1_D = OrganiseringState.builder()
        .fid("FID_D1_D")
        .id(D1_ENHET.id())
        .navn(D1_ENHET.navn())
        .idOver(D_ENHET.id())
        .navnOver(D_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_E_A = OrganiseringState.builder()
        .fid("FID_E_A")
        .id(E_ENHET.id())
        .navn(E_ENHET.navn())
        .idOver(A_ENHET.id())
        .navnOver(A_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

    public final OrganiseringState organisering_E1_E = OrganiseringState.builder()
        .fid("FID_E1_E")
        .id(E1_ENHET.id())
        .navn(E1_ENHET.navn())
        .idOver(E_ENHET.id())
        .navnOver(E_ENHET.navn())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .build();

        /*
            =======     ressurser, ledere
     */


    public final String MEDARBEIDER_A_NAVIDENT = "A000000";
    public final RessursState MEDARBEIDER_A = RessursState.builder().navident(MEDARBEIDER_A_NAVIDENT).sektor(Sektor.NAV_STATLIG)
        .visningsnavn(new Visningsnavn("Navn", Optional.empty(), MEDARBEIDER_A_NAVIDENT))
        .build();


    public final String MEDARBEIDER_A1_NAVIDENT = "A100000";
    public final RessursState MEDARBEIDER_A1 = RessursState.builder().navident(MEDARBEIDER_A1_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_A11_NAVIDENT = "A110000";
    public final RessursState MEDARBEIDER_A11 = RessursState.builder().navident(MEDARBEIDER_A11_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_B_NAVIDENT = "B000000";
    public final RessursState MEDARBEIDER_B = RessursState.builder()
        .navident(MEDARBEIDER_B_NAVIDENT)
        .sektor(Sektor.NAV_STATLIG)
        .visningsnavn(new Visningsnavn("Navn", Optional.empty(), MEDARBEIDER_B_NAVIDENT))
        .build();


    public final String MEDARBEIDER_B1_NAVIDENT = "B100000";
    public final RessursState MEDARBEIDER_B1 = RessursState.builder().navident(MEDARBEIDER_B1_NAVIDENT).sektor(Sektor.NAV_STATLIG)
        .visningsnavn(new Visningsnavn("Navn", Optional.empty(), MEDARBEIDER_B1_NAVIDENT))
        .build();


    public final String MEDARBEIDER_C_NAVIDENT = "C000000";
    public final RessursState MEDARBEIDER_C = RessursState.builder().navident(MEDARBEIDER_C_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_C1_NAVIDENT = "C100000";
    public final RessursState MEDARBEIDER_C1 = RessursState.builder().navident(MEDARBEIDER_C1_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_C3_NAVIDENT = "C300000";
    public final RessursState MEDARBEIDER_C3 = RessursState.builder().navident(MEDARBEIDER_C3_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_C2_NAVIDENT = "C200000";
    public final RessursState MEDARBEIDER_C2 = RessursState.builder().navident(MEDARBEIDER_C2_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_D_NAVIDENT = "D000000";
    public final RessursState MEDARBEIDER_D = RessursState.builder().navident(MEDARBEIDER_D_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_H1_NAVIDENT = "H100000";
    public final RessursState MEDARBEIDER_H1 = RessursState.builder().navident(MEDARBEIDER_H1_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_R1_NAVIDENT = "R100000";
    public final RessursState MEDARBEIDER_R1 = RessursState.builder().navident(MEDARBEIDER_R1_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_S1_NAVIDENT = "S100000";
    public final RessursState MEDARBEIDER_S1 = RessursState.builder().navident(MEDARBEIDER_S1_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


            /*
            =======     ressurser, ikke ledere
     */


    public final String MEDARBEIDER_B110001_NAVIDENT = "B110001";
    public final RessursState MEDARBEIDER_B110001 = RessursState.builder()
        .navident(MEDARBEIDER_B110001_NAVIDENT)
        .sektor(Sektor.NAV_STATLIG)
        .visningsnavn(new Visningsnavn("Navn", Optional.empty(), MEDARBEIDER_B110001_NAVIDENT))
        .build();


    public final String MEDARBEIDER_B110002_NAVIDENT = "B110002";
    public final RessursState MEDARBEIDER_B110002 = RessursState.builder().navident(MEDARBEIDER_B110002_NAVIDENT).sektor(Sektor.NAV_STATLIG)
        .visningsnavn(new Visningsnavn("Navn", Optional.empty(), MEDARBEIDER_B110002_NAVIDENT))
        .build();


    public final String MEDARBEIDER_C110001_NAVIDENT = "C110001";
    public final RessursState MEDARBEIDER_C110001 = RessursState.builder().navident(MEDARBEIDER_C110001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_C220001_NAVIDENT = "C220001";
    public final RessursState MEDARBEIDER_C220001 = RessursState.builder().navident(MEDARBEIDER_C220001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_D100001_NAVIDENT = "D100001";
    public final RessursState MEDARBEIDER_D100001 = RessursState.builder().navident(MEDARBEIDER_D100001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_E100001_NAVIDENT = "E100001";
    public final RessursState MEDARBEIDER_E100001 = RessursState.builder().navident(MEDARBEIDER_E100001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_EKSTERN_NAVIDENT = "E999000";
    public final RessursState MEDARBEIDER_EKSTERN = RessursState.builder().navident(MEDARBEIDER_EKSTERN_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_H100001_NAVIDENT = "H100001";
    public final RessursState MEDARBEIDER_H100001 = RessursState.builder().navident(MEDARBEIDER_H100001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_HR_NAVIDENT = "H999000";
    public final RessursState MEDARBEIDER_HR = RessursState.builder().navident(MEDARBEIDER_HR_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_R100001_NAVIDENT = "R100001";
    public final RessursState MEDARBEIDER_R100001 = RessursState.builder().navident(MEDARBEIDER_R100001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    public final String MEDARBEIDER_S100001_NAVIDENT = "S100001";
    public final RessursState MEDARBEIDER_S100001 = RessursState.builder().navident(MEDARBEIDER_S100001_NAVIDENT).sektor(Sektor.NAV_STATLIG).build();


    /*
            =======     orgtilknytninger
     */


    public final OrgtilknytningState ot_B11_B110001 = OrgtilknytningState.builder()
        .fid("FID_B11_B110001")
        .navident(MEDARBEIDER_B110001.navident())
        .orgEnhetId(B11_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_B11_B110002 = OrgtilknytningState.builder()
        .fid("FID_B11_B110002")
        .navident(MEDARBEIDER_B110002_NAVIDENT)
        .orgEnhetId(B11_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_B1_B1 = OrgtilknytningState.builder()
        .fid("FID_B1_B1")
        .navident(MEDARBEIDER_B1.navident())
        .orgEnhetId(B1_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_B_B = OrgtilknytningState.builder()
        .fid("FID_B_B")
        .navident(MEDARBEIDER_B.navident())
        .orgEnhetId(B_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_C11_C110001 = OrgtilknytningState.builder()
        .fid("FID_C11_C10001")
        .navident(MEDARBEIDER_C110001_NAVIDENT)
        .orgEnhetId(C11_ENHET_NOMID)
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_C22_C220001 = OrgtilknytningState.builder()
        .fid("FID_C22_C220001")
        .navident(MEDARBEIDER_C220001_NAVIDENT)
        .orgEnhetId(C22_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_D1_D100001 = OrgtilknytningState.builder()
        .fid("FID_D1_D100001")
        .navident(MEDARBEIDER_D100001_NAVIDENT)
        .orgEnhetId(D1_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_E1_E100001 = OrgtilknytningState.builder()
        .fid("FID_E1_E100001")
        .navident(MEDARBEIDER_E100001_NAVIDENT)
        .orgEnhetId(E1_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(5))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_HELFO_H100001 = OrgtilknytningState.builder()
        .fid("FID_HELFO_H100001")
        .navident(MEDARBEIDER_H100001_NAVIDENT)
        .orgEnhetId(HELFO_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(10))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_SKATT_S100001 = OrgtilknytningState.builder()
        .fid("FID_SKATT_S100001")
        .navident(MEDARBEIDER_S100001_NAVIDENT)
        .orgEnhetId(SKATT_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(10))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();

    public final OrgtilknytningState ot_RIKSREV_R100001 = OrgtilknytningState.builder()
        .fid("FID_RIKSREV_R100001")
        .navident(MEDARBEIDER_R100001_NAVIDENT)
        .orgEnhetId(RIKSREV_ENHET.id())
        .gyldigFom(LocalDate.now().minusDays(10))
        .gyldigTom(null)
        .erDagligOppfolging(true)
        .build();


    /*
            =======     leder state
     */
    public final OrgEnhetLederState LEDER_A = OrgEnhetLederState.builder()
        .fid("LEDER_A")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(A_ENHET.id())
        .navident(MEDARBEIDER_A.navident())
        .build();


    public final OrgEnhetLederState LEDER_A1 = OrgEnhetLederState.builder()
        .fid("LEDER_A1")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(A1_ENHET.id())
        .navident(MEDARBEIDER_A11.navident())
        .build();

    public final OrgEnhetLederState LEDER_A11 = OrgEnhetLederState.builder()
        .fid("LEDER_A11")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(A11_ENHET.id())
        .navident(MEDARBEIDER_A11.navident())
        .build();

    public final OrgEnhetLederState LEDER_B = OrgEnhetLederState.builder()
        .fid("LEDER_B")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(B_ENHET.id())
        .navident(MEDARBEIDER_B.navident())
        .build();

    public final OrgEnhetLederState LEDER_B1 = OrgEnhetLederState.builder()
        .fid("LEDER_B1")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(B1_ENHET.id())
        .navident(MEDARBEIDER_B1.navident())
        .build();

    public final OrgEnhetLederState LEDER_B11 = OrgEnhetLederState.builder()
        .fid("LEDER_B11")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(B11_ENHET.id())
        .navident(MEDARBEIDER_B1.navident())
        .build();

    public final OrgEnhetLederState LEDER_C = OrgEnhetLederState.builder()
        .fid("LEDER_C")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(C_ENHET.id())
        .navident(MEDARBEIDER_C.navident())
        .build();

    public final OrgEnhetLederState LEDER_C1 = OrgEnhetLederState.builder()
        .fid("LEDER_C1")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(C1_ENHET.id())
        .navident(MEDARBEIDER_C.navident())
        .build();

    public final OrgEnhetLederState LEDER_C11 = OrgEnhetLederState.builder()
        .fid("LEDER_C11")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(C11_ENHET.id())
        .navident(MEDARBEIDER_C3.navident())
        .build();

    public final OrgEnhetLederState LEDER_C2 = OrgEnhetLederState.builder()
        .fid("LEDER_C2")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(C2_ENHET.id())
        .navident(MEDARBEIDER_C2.navident())
        .build();

    public final OrgEnhetLederState LEDER_C22 = OrgEnhetLederState.builder()
        .fid("LEDER_C22")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(C22_ENHET.id())
        .navident(MEDARBEIDER_C2.navident())
        .build();

    public final OrgEnhetLederState LEDER_D = OrgEnhetLederState.builder()
        .fid("LEDER_D")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(D_ENHET.id())
        .navident(MEDARBEIDER_D.navident())
        .build();

    public final OrgEnhetLederState LEDER_D1 = OrgEnhetLederState.builder()
        .fid("LEDER_D1")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(D1_ENHET.id())
        .navident(MEDARBEIDER_D.navident())
        .build();

    public final OrgEnhetLederState LEDER_E = OrgEnhetLederState.builder()
        .fid("LEDER_E")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(E_ENHET.id())
        .navident(MEDARBEIDER_D.navident())
        .build();

    public final OrgEnhetLederState LEDER_E1 = OrgEnhetLederState.builder()
        .fid("LEDER_E1")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(E1_ENHET.id())
        .navident(MEDARBEIDER_EKSTERN.navident()) // todo sjekk hvem som egentlig skal være leder her
        .build();

    public final OrgEnhetLederState LEDER_HELFO = OrgEnhetLederState.builder()
        .fid("LEDER_HELFO")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(HELFO_ENHET.id())
        .navident(MEDARBEIDER_H1.navident())
        .build();

    public final OrgEnhetLederState LEDER_SKATT = OrgEnhetLederState.builder()
        .fid("LEDER_SKATT")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(SKATT_ENHET.id())
        .navident(MEDARBEIDER_S1.navident())
        .build();

    public final OrgEnhetLederState LEDER_RIKSREV = OrgEnhetLederState.builder()
        .fid("LEDER_RIKSREV")
        .startdato(LocalDate.now().minusDays(5))
        .sluttdato(null)
        .orgEnhetId(RIKSREV_ENHET.id())
        .navident(MEDARBEIDER_R1.navident())
        .build();


    @Override
    public void upsertInitHierarki(HierarkiService.Upserts upserts, Long seed) {
        var organiseringer = List.of(organisering_A_X,
            organisering_A1_A,
            organisering_A11_A1,
            organisering_B_A,
            organisering_B1_B,
            organisering_B11_B1,
            organisering_C_A,
            organisering_C1_C,
            organisering_C11_C1,
            organisering_C2_C,
            organisering_C22_C2,
            organisering_D_A,
            organisering_D1_D,
            organisering_E_A,
            organisering_E1_E);

        var enheter = List.of(
            X_ENHET,
            A_ENHET,
            A1_ENHET,
            A11_ENHET,
            B_ENHET,
            B1_ENHET,
            B11_ENHET,
            C_ENHET,
            C1_ENHET,
            C11_ENHET,
            C2_ENHET,
            C22_ENHET,
            D_ENHET,
            D1_ENHET,
            E_ENHET,
            E1_ENHET,
            HELFO_ENHET,
            SKATT_ENHET
        );

        var ressurser = List.of(
            MEDARBEIDER_A, MEDARBEIDER_A1, MEDARBEIDER_A11, MEDARBEIDER_B, MEDARBEIDER_B1,
            MEDARBEIDER_C, MEDARBEIDER_C1, MEDARBEIDER_C3, MEDARBEIDER_C2, MEDARBEIDER_D, MEDARBEIDER_H1,
            MEDARBEIDER_R1, MEDARBEIDER_S1);



        var ressurserIkkeLedere = List.of(MEDARBEIDER_B110001,
            MEDARBEIDER_B110002,
            MEDARBEIDER_C110001,
            MEDARBEIDER_C220001,
            MEDARBEIDER_D100001,
            MEDARBEIDER_E100001,
            MEDARBEIDER_EKSTERN,
            MEDARBEIDER_H100001,
            MEDARBEIDER_HR,
            MEDARBEIDER_R100001,
            MEDARBEIDER_S100001
        );

        var orgtilknytninger = List.of(
            ot_B11_B110001 ,
            ot_B11_B110002 ,
            ot_B1_B1 ,
            ot_B_B,
            ot_C11_C110001 ,
            ot_C22_C220001 ,
            ot_D1_D100001 ,
            ot_E1_E100001 ,
            ot_HELFO_H100001 ,
            ot_SKATT_S100001 ,
            ot_RIKSREV_R100001
        );

        var ledere = List.of(
            LEDER_A,
            LEDER_A1,
            LEDER_A11,
            LEDER_B,
            LEDER_B1,
            LEDER_B11,
            LEDER_C,
            LEDER_C1,
            LEDER_C11,
            LEDER_C2,
            LEDER_C22,
            LEDER_D,
            LEDER_D1,
            LEDER_E,
            LEDER_E1,
            LEDER_HELFO,
            LEDER_SKATT,
            LEDER_RIKSREV
        );


        var allTogether = Streams.concat(
                ressurserIkkeLedere.stream(),
                ressurser.stream(),
                orgtilknytninger.stream(),
                enheter.stream(),
                ledere.stream()
                // organiseringer.stream() // todo, add handling of organiseringer
        ).toList();

        upsertItemsInRandomOrder(allTogether, upserts, System.currentTimeMillis());
    }
}
