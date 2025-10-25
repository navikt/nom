package no.nav.nom.apps.api.hierarki;

import no.nav.nom.apps.api.hierarki.initals.InitHierarkiStandard;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Denne testen baserer seg på et standard hierarki som er forholdsvis stort, men
 * ingen periodsering blir testet
 *
 * todo: lag en annen test der også periodisering blir testet, f.eks at orgtilknytning kan variere
 *
 */
class HierarkiServiceTest {

    /*
     * Setup
     */

    private final HierarkiService hierarkiService;

    private static final InitHierarkiStandard H = new InitHierarkiStandard();

    @NotNull
    private HierarkiOrgenhet hentOgVerifiserEnhetB11() {
        var res = hierarkiService.enhetByNomId(NomId.create(H.B11_ENHET_NOMID));
        assertThat(res).isNotEmpty();
        return res.get();
    }


    public HierarkiServiceTest() {
        // om upserts trekkes ut, så kan nye state oppdateringer sendes underveis i testene
        var upserts = new HierarkiService.Upserts() {};
        this.hierarkiService = new HierarkiService(upserts);
        H.upsertInitHierarki(upserts);
    }

    /*
    * TESTER
     */

    @Test
    void medarbeiderByNavident() {
        var res = hierarkiService.medarbeiderByNavident(new NavIdent(H.MEDARBEIDER_A_NAVIDENT));
        assertThat(res).isNotEmpty();
    }

    @Test
    void enhetByNomId() {
        var res = hierarkiService.enhetByNomId(NomId.create(H.A_ENHET_NOMID));
        assertThat(res).isNotEmpty();
    }

    @Test
    @DisplayName("Kan hente ressurser på enheter slik vi forventer")
    void folkPaaEnEnhet() {
        var enhet = hentOgVerifiserEnhetB11();

        List<Medarbeider> medarbeiderList = enhet.medarbeiderePaaEnhet();
        assertThat(medarbeiderList).hasSize(2);
        assertThat(medarbeiderList.stream().map(it -> it.toMinimalRessurs().navident())).containsExactlyInAnyOrder(
            H.MEDARBEIDER_B110001_NAVIDENT,
            H.MEDARBEIDER_B110002_NAVIDENT
        );
    }

    @Test
    @DisplayName("Kan hente en enhet sin leder")
    void lederPaaEnhet() {
        var enhet = hentOgVerifiserEnhetB11();

        Medarbeider leder = enhet.enhetLeder();
        assertThat(leder).isNotNull();
        assertThat(leder.toMinimalRessurs().navident()).isEqualTo(H.MEDARBEIDER_B1.navident());
    }

    @Test
    @DisplayName("Kan hente en enhet sin leder (2)")
    void lederPaaEnhe2t() {
        var enhet = this.hierarkiService.enhetByNomId(NomId.create(H.A_ENHET_NOMID)).get();

        Medarbeider leder = enhet.enhetLeder();
        assertThat(leder).isNotNull();
        assertThat(leder.toMinimalRessurs().navident()).isEqualTo(H.MEDARBEIDER_A.navident());
    }

    @Test
    @DisplayName("Kan hente en ressurs sin orgenhet")
    void ressursSinOrgEnhet() {
        var medarbeiderMaybe = this.hierarkiService.medarbeiderByNavident(NavIdent.create(H.MEDARBEIDER_B110001_NAVIDENT));
        assertThat(medarbeiderMaybe).isNotEmpty();
        var medarbeider = medarbeiderMaybe.get();

        HierarkiOrgenhet orgenhet = medarbeider.orgenhet();
        assertThat(orgenhet).isNotNull();
        assertThat(orgenhet.id().nomId()).isEqualTo(H.B11_ENHET.id());
    }

}