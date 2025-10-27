package no.nav.nom.apps.api.hierarki;

import no.nav.nom.apps.api.hierarki.initals.InitHierarkiByBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class HierarkiService2Test {

    private final HierarkiService hierarkiService;

    private static final InitHierarkiByBuilder H = new InitHierarkiByBuilder();

    public HierarkiService2Test() {
        // om upserts trekkes ut, så kan nye state oppdateringer sendes underveis i testene
        var upserts = new HierarkiService.Upserts() {};
        this.hierarkiService = new HierarkiService(upserts);
        H.upsertInitHierarki(upserts);
    }

    @Test
    public void henteEnhetViaEnAnsatt(){
        var nesnaRessurs = hierarkiService.medarbeiderByNavident(NavIdent.create(H.ANSATTE_SORTLAND.getFirst())).orElseThrow();
        var nesnaEnhet = nesnaRessurs.orgenhet(LocalDate.now());
        assertThat(nesnaEnhet).isNotNull();
        assertThat(nesnaEnhet.id().nomId()).isEqualTo(H.ENHET_SORTLAND_7S.id());
    }

    @Test
    public void henteLeder(){
        var now = LocalDate.now();
        var navEnhet = hierarkiService.enhetByNomId(NomId.create(H.ENHET_NAV)).orElseThrow();
        var leder = navEnhet.enhetLeder(now);
        assertThat(leder).isNotNull();
        assertThat(leder.toMinimalRessurs().navident()).isEqualTo(H.LEDER_NAV.navident());
    }

    @Test
    public void henteOverEnhet(){
        var nesnaEnhet = hierarkiService.enhetByNomId(NomId.create(H.ENHET_SORTLAND_7S)).orElseThrow();
        var over = nesnaEnhet.orgenhetOver(LocalDate.now());
        assertThat(over).isNotNull();
        assertThat(over.id()).isNotEqualTo(nesnaEnhet.id());
    }

    @Test
    @DisplayName("medarbeider.leders leder (henting)")
    void medarbeiderSinLedersLeder() {
        var now = LocalDate.now();
        var medarbeider = this.hierarkiService.medarbeiderByNavident(NavIdent.create(H.ANSATTE_SORTLAND.getFirst())).orElseThrow();

        var leder = medarbeider.leder(now);
        assertThat(leder).isNotNull();
        assertThat(leder.navIdent().navIdent()).isEqualTo(H.LEDER_SORTLAND_7S.navident());

        var ledersLeder = leder.leder(now);
        assertThat(ledersLeder.sameAs(leder)).isFalse();

        var expectedLedersLeder = this.hierarkiService.medarbeiderByNavident(NavIdent.create(H.LEDER_SORTLAND)).orElseThrow();

        assertThat(ledersLeder).isNotNull();
        assertThat(ledersLeder.navIdent().navIdent()).isEqualTo(expectedLedersLeder.navIdent().navIdent());
    }

    @Test
    @DisplayName("Leder for")
    void lederFor(){
        var now = LocalDate.now();
        var medarbeider = this.hierarkiService.medarbeiderByNavident(NavIdent.create(H.LEDER_NARVIK)).orElseThrow();

        var enheter = medarbeider.lederFor(now).stream().map(it -> it.backingState).toList();
        assertThat(enheter).isNotNull();
        assertThat(enheter).hasSize(2);
        assertThat(enheter).containsExactlyInAnyOrder(H.ENHET_NARVIK_7S, H.ENHET_NARVIK);
    }


}
