package no.nav.nom.libs.state.organisering;

import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrganiseringStateRepositoryOrganieringerOverOgUnderTest {
    private final OrganiseringStateRepository organiseringStateRepository = new OrganiseringStateRepositoryImpl();

    // Enhet id A til H
    private static final String ENHET_A = "enhet_A";
    private static final String ENHET_B = "enhet_B";
    private static final String ENHET_C = "enhet_C";
    private static final String ENHET_D = "enhet_D";
    private static final String ENHET_E = "enhet_E";
    private static final String ENHET_F = "enhet_F";
    private static final String ENHET_G = "enhet_G";
    private static final String ENHET_H = "enhet_H";

    private static final String ENHET_X = "enhet_X"; // utenfor hovedtreet
    private static final String ENHET_X2 = "enhet_X2";

    LocalDate historiskFom = LocalDate.now().minusYears(2);
    LocalDate historiskTom = LocalDate.now().minusYears(1).minusDays(1);
    LocalDate aktivFom = LocalDate.now().minusYears(1);
    LocalDate aktivTom = LocalDate.now().plusYears(1).minusDays(1);
    LocalDate fremtidigFom = LocalDate.now().plusYears(1);
    LocalDate fremtidigTom = LocalDate.now().plusYears(2).minusDays(1);

    /*
      Lag organiseringer

      Historisk:               Aktiv:               Fremtidig
        X         A        X       A            X       A
        |      ___|_       |    ___|_           |    ___|_
        X2   |     |       X2  |     |          X2  |     |
             B     C           B     C              B     C
           __|_                |     |            __|_    |
          |    |               D     E           |    |   E
          D    E                     |           F    G   |
               |                     H                    H
               H
    */

    OrganiseringState organisering_B   = new OrganiseringState("fid-B",   ENHET_B,  null, ENHET_A, null, historiskFom, null);
    OrganiseringState organisering_C   = new OrganiseringState("fid-C",   ENHET_C,  null, ENHET_A, null, historiskFom, null);
    OrganiseringState organisering_D   = new OrganiseringState("fid-D",   ENHET_D,  null, ENHET_B, null, historiskFom, aktivTom);
    OrganiseringState organisering_E_1 = new OrganiseringState("fid-E_1", ENHET_E,  null, ENHET_B, null, historiskFom, historiskTom);
    OrganiseringState organisering_E_2 = new OrganiseringState("fid-E_2", ENHET_E,  null, ENHET_C, null, aktivFom,     null);
    OrganiseringState organisering_F   = new OrganiseringState("fid-F",   ENHET_F,  null, ENHET_B, null, fremtidigFom, null);
    OrganiseringState organisering_G   = new OrganiseringState("fid-G",   ENHET_G,  null, ENHET_B, null, fremtidigFom, null);
    OrganiseringState organisering_H   = new OrganiseringState("fid-H",   ENHET_H,  null, ENHET_E, null, historiskFom, null);

    OrganiseringState organisering_X2  = new OrganiseringState("fid-X2",  ENHET_X2, null, ENHET_X, null, historiskFom, fremtidigTom);

    private void setupDefaultOrganiseringer() {
        organiseringStateRepository.update(organisering_B);
        organiseringStateRepository.update(organisering_C);
        organiseringStateRepository.update(organisering_D);
        organiseringStateRepository.update(organisering_E_1);
        organiseringStateRepository.update(organisering_E_2);
        organiseringStateRepository.update(organisering_F);
        organiseringStateRepository.update(organisering_G);
        organiseringStateRepository.update(organisering_H);

        organiseringStateRepository.update(organisering_X2);
    }

    @BeforeEach
    public void setup() {
        organiseringStateRepository.clear();
    }

    @Test
    public void verify_default_setup_all_organiseringer_nedover() {
        setupDefaultOrganiseringer();

        assertEquals(9, organiseringStateRepository.count());
        assertEquals(8, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_A).size());
        assertEquals(5, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_B).size());
        assertEquals(2, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_C).size());
        assertEquals(0, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_D).size());
        assertEquals(1, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_E).size());
        assertEquals(0, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_F).size());
        assertEquals(0, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_G).size());
        assertEquals(0, organiseringStateRepository.getEnhetOgOrganiseringerNedover(ENHET_H).size());

        assertEquals(7, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_A).size());
        assertEquals(5, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_B).size());
        assertEquals(2, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_C).size());
        assertEquals(0, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_D).size());
        assertEquals(1, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_E).size());
        assertEquals(0, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_F).size());
        assertEquals(0, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_G).size());
        assertEquals(0, organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_H).size());

        assertTrue(organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_A).containsAll(Set.of(ENHET_B, ENHET_C, ENHET_D, ENHET_E, ENHET_F, ENHET_G, ENHET_H)));
        assertTrue(organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_B).containsAll(Set.of(ENHET_D, ENHET_E, ENHET_F, ENHET_G, ENHET_H)));
        assertTrue(organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_C).containsAll(Set.of(ENHET_E, ENHET_H)));
        assertTrue(organiseringStateRepository.getChildrenEnhetIdsFlattened(ENHET_E).contains(ENHET_H));
    }

    @Test
    public void verify_default_setup_all_organiseringer_oppover() {
        setupDefaultOrganiseringer();

        assertEquals(9, organiseringStateRepository.count());
        assertEquals(0, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_A).size());
        assertEquals(1, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_B).size());
        assertEquals(1, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_C).size());
        assertEquals(2, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_D).size());
        assertEquals(4, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_E).size());
        assertEquals(2, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_F).size());
        assertEquals(2, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_G).size());
        assertEquals(5, organiseringStateRepository.getEnhetOgOrganiseringerOppover(ENHET_H).size());

        assertEquals(0, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_A).size());
        assertEquals(1, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_B).size());
        assertEquals(1, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_C).size());
        assertEquals(2, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_D).size());
        assertEquals(3, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_E).size());
        assertEquals(2, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_F).size());
        assertEquals(2, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_G).size());
        assertEquals(4, organiseringStateRepository.getParentEnhetIdsFlattened(ENHET_H).size());
    }

    @Test
    public void verify_default_setup_lovnoder_under() {
        setupDefaultOrganiseringer();

        assertEquals(9, organiseringStateRepository.count());
        assertEquals(5, organiseringStateRepository.getAlleLovnodeIder().size());

        assertEquals(4, organiseringStateRepository.getLovnoderIderUnder(ENHET_A).size());
        assertEquals(4, organiseringStateRepository.getLovnoderIderUnder(ENHET_B).size());
        assertEquals(1, organiseringStateRepository.getLovnoderIderUnder(ENHET_C).size());
        assertEquals(0, organiseringStateRepository.getLovnoderIderUnder(ENHET_D).size());
        assertEquals(1, organiseringStateRepository.getLovnoderIderUnder(ENHET_E).size());
        assertEquals(0, organiseringStateRepository.getLovnoderIderUnder(ENHET_F).size());
        assertEquals(0, organiseringStateRepository.getLovnoderIderUnder(ENHET_G).size());
        assertEquals(0, organiseringStateRepository.getLovnoderIderUnder(ENHET_H).size());
        assertEquals(1, organiseringStateRepository.getLovnoderIderUnder(ENHET_X).size());

        assertEquals(4, organiseringStateRepository.getLovnoderUnder(ENHET_A).size());
        assertEquals(4, organiseringStateRepository.getLovnoderUnder(ENHET_B).size());
        assertEquals(1, organiseringStateRepository.getLovnoderUnder(ENHET_C).size());
        assertEquals(0, organiseringStateRepository.getLovnoderUnder(ENHET_D).size());
        assertEquals(1, organiseringStateRepository.getLovnoderUnder(ENHET_E).size());
        assertEquals(0, organiseringStateRepository.getLovnoderUnder(ENHET_F).size());
        assertEquals(0, organiseringStateRepository.getLovnoderUnder(ENHET_G).size());
        assertEquals(0, organiseringStateRepository.getLovnoderUnder(ENHET_H).size());
        assertEquals(1, organiseringStateRepository.getLovnoderUnder(ENHET_X).size());
    }
}
