package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.libs.gql.projections.Pagineringsparametre;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagineringTest {

    private static List<Integer> elementer(int antall) {
        return IntStream.range(0, antall).boxed().toList();
    }

    @Test
    void foerste_side_av_2000_med_sidestoerrelse_100_gir_20_sider() {
        var side = Paginering.paginer(elementer(2000), new Pagineringsparametre(0, 100));

        assertThat(side.totalCount()).isEqualTo(2000);
        assertThat(side.totalPages()).isEqualTo(20);
        assertThat(side.pageNumber()).isZero();
        assertThat(side.pageSize()).isEqualTo(100);
        assertThat(side.numberOfElements()).isEqualTo(100);
        assertThat(side.first()).isTrue();
        assertThat(side.last()).isFalse();
        assertThat(side.hasPreviousPage()).isFalse();
        assertThat(side.hasNextPage()).isTrue();
        assertThat(side.innhold()).first().isEqualTo(0);
        assertThat(side.innhold()).last().isEqualTo(99);
    }

    @Test
    void siste_hele_side() {
        var side = Paginering.paginer(elementer(2000), new Pagineringsparametre(19, 100));

        assertThat(side.numberOfElements()).isEqualTo(100);
        assertThat(side.first()).isFalse();
        assertThat(side.last()).isTrue();
        assertThat(side.hasPreviousPage()).isTrue();
        assertThat(side.hasNextPage()).isFalse();
        assertThat(side.innhold()).first().isEqualTo(1900);
        assertThat(side.innhold()).last().isEqualTo(1999);
    }

    @Test
    void delvis_siste_side() {
        var side = Paginering.paginer(elementer(1999), new Pagineringsparametre(19, 100));

        assertThat(side.totalPages()).isEqualTo(20);
        assertThat(side.numberOfElements()).isEqualTo(99);
        assertThat(side.last()).isTrue();
        assertThat(side.hasNextPage()).isFalse();
    }

    @Test
    void ett_element_over_gir_en_ekstra_side() {
        var side = Paginering.paginer(elementer(2001), new Pagineringsparametre(0, 100));

        assertThat(side.totalPages()).isEqualTo(21);
    }

    @Test
    void tomt_resultat() {
        var side = Paginering.paginer(elementer(0), new Pagineringsparametre(0, 100));

        assertThat(side.totalCount()).isZero();
        assertThat(side.totalPages()).isZero();
        assertThat(side.numberOfElements()).isZero();
        assertThat(side.innhold()).isEmpty();
        assertThat(side.first()).isTrue();
        assertThat(side.last()).isTrue();
        assertThat(side.hasNextPage()).isFalse();
        assertThat(side.hasPreviousPage()).isFalse();
    }

    @Test
    void side_utenfor_rekkevidde_gir_tomt_innhold_men_korrekt_metadata() {
        var side = Paginering.paginer(elementer(2000), new Pagineringsparametre(25, 100));

        assertThat(side.totalCount()).isEqualTo(2000);
        assertThat(side.totalPages()).isEqualTo(20);
        assertThat(side.numberOfElements()).isZero();
        assertThat(side.innhold()).isEmpty();
        assertThat(side.hasNextPage()).isFalse();
        assertThat(side.hasPreviousPage()).isTrue();
        assertThat(side.last()).isTrue();
    }

    @Test
    void valider_avviser_negativt_sidenummer() {
        assertThatThrownBy(() -> Paginering.valider(new Pagineringsparametre(-1, 100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pageNumber");
    }

    @Test
    void valider_avviser_sidestoerrelse_under_1() {
        assertThatThrownBy(() -> Paginering.valider(new Pagineringsparametre(0, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pageSize");
    }

    @Test
    void valider_avviser_sidestoerrelse_over_maks() {
        assertThatThrownBy(() -> Paginering.valider(new Pagineringsparametre(0, Pagineringsparametre.MAKS_SIDESTORRELSE + 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("overstige");
    }

    @Test
    void valider_godtar_gyldige_parametre() {
        Paginering.valider(new Pagineringsparametre(0, 100));
        Paginering.valider(new Pagineringsparametre(5, Pagineringsparametre.MAKS_SIDESTORRELSE));
    }
}
