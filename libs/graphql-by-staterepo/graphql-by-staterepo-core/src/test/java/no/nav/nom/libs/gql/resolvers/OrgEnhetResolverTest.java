package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetSide;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.gql.NomGraphqlSupportInterface;
import no.nav.nom.libs.gql.projections.OrgEnheterSearch;
import no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter;
import no.nav.nom.libs.gql.projections.Pagineringsparametre;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;
import static no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter.LovnodeSelection.ALLE;
import static no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter.LovnodeSelection.IKKE_LOVNODE_ENHET;
import static no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter.LovnodeSelection.LOVNODE_ENHET;
import static no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter.OrganiseringSelection.UNDER_ANGITT_ORGENHET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrgEnhetResolverTest {

    @Test
    void aktive_historiske_og_fremtidige_enheter_og_organiseringer_filtreres_samlet() {
        var fixture = new Fixture();
        var yesterday = LocalDate.now().minusDays(1);
        var tomorrow = LocalDate.now().plusDays(1);

        fixture.enhet("root", LocalDate.now().minusYears(5), null);
        fixture.enhet("active", LocalDate.now().minusMonths(2), null);
        fixture.enhet("historic", LocalDate.now().minusYears(2), yesterday);
        fixture.enhet("future", tomorrow, null);
        fixture.organisering("active-edge", "active", "root", LocalDate.now().minusMonths(2), null);
        fixture.organisering("historic-edge", "historic", "root", LocalDate.now().minusYears(2), yesterday);
        fixture.organisering("future-edge", "future", "root", tomorrow, null);

        assertThat(fixture.ids("root", AKTIVE)).containsExactlyInAnyOrder("root", "active");
        assertThat(fixture.ids("root", PeriodiskUtvalgArg.HISTORISKE)).containsExactlyInAnyOrder("historic");
        assertThat(fixture.ids("root", PeriodiskUtvalgArg.FREMTIDIGE)).containsExactlyInAnyOrder("future");
    }

    @Test
    void aktive_enheter_under_angitt_orgenhet_med_lovnodefilter() {
        var fixture = new Fixture();
        fixture.enhet("root", LocalDate.now().minusYears(5), null);
        fixture.enhet("branch", LocalDate.now().minusYears(2), null);
        fixture.enhet("leaf", LocalDate.now().minusYears(1), null);
        fixture.enhet("other-leaf", LocalDate.now().minusYears(1), null);
        fixture.organisering("branch-edge", "branch", "root", LocalDate.now().minusYears(2), null);
        fixture.organisering("leaf-edge", "leaf", "branch", LocalDate.now().minusYears(1), null);
        fixture.organisering("other-leaf-edge", "other-leaf", "root", LocalDate.now().minusYears(1), null);

        assertThat(fixture.ids("root", AKTIVE, LOVNODE_ENHET)).containsExactlyInAnyOrder("leaf", "other-leaf");
        assertThat(fixture.ids("root", AKTIVE, IKKE_LOVNODE_ENHET)).containsExactlyInAnyOrder("root", "branch");
    }

    @Test
    void alle_returnerer_enheter_uavhengig_av_organisering() {
        var fixture = new Fixture();
        fixture.enhet("root", LocalDate.now().minusYears(5), null);
        fixture.enhet("organized", LocalDate.now().minusYears(1), null);
        fixture.enhet("unorganized", LocalDate.now().minusYears(1), null);
        fixture.organisering("organized-edge", "organized", "root", LocalDate.now().minusYears(1), null);

        var result = fixture.resolver.orgEnheter(
            OrgEnheterSearch.EMPTY_SEARCH,
            filter(OrgEnheterSearchFilter.OrganiseringSelection.ALLE, null, ALLE, PeriodiskUtvalgArg.ALLE)
        );

        assertThat(ids(result)).containsExactlyInAnyOrder("root", "organized", "unorganized");
    }

    @Test
    void eksplisitte_ider_returnerer_ok_not_found_og_not_in_filter() {
        var fixture = new Fixture();
        fixture.enhet("root", LocalDate.now().minusYears(5), null);
        fixture.enhet("inside", LocalDate.now().minusYears(1), null);
        fixture.enhet("outside", LocalDate.now().minusYears(1), null);
        fixture.organisering("inside-edge", "inside", "root", LocalDate.now().minusYears(1), null);

        var search = new OrgEnheterSearch(null, null, null, null, null, Set.of("inside", "outside", "missing"));
        var results = fixture.resolver.orgEnheter(search, filter(UNDER_ANGITT_ORGENHET, "root", ALLE, AKTIVE));
        var resultById = results.stream().collect(Collectors.toMap(OrgEnhetResult::getNomId, Function.identity()));

        assertThat(resultById.get("inside").getCode()).isEqualTo(ResultCode.OK);
        assertThat(resultById.get("outside").getCode()).isEqualTo(ResultCode.NOT_IN_FILTER);
        assertThat(resultById.get("missing").getCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void flyttet_enhet_vises_under_ny_men_ikke_gammel_overenhet() {
        var fixture = new Fixture();
        var yesterday = LocalDate.now().minusDays(1);
        var start = LocalDate.now().minusYears(1);
        fixture.enhet("old-root", start, null);
        fixture.enhet("new-root", start, null);
        fixture.enhet("moved", start, null);
        fixture.enhet("descendant", start, null);

        fixture.organisering("old-historical-edge", "moved", "old-root", start, yesterday);
        fixture.organisering("descendant-edge", "descendant", "moved", start, null);
        fixture.organisering("new-edge", "moved", "new-root", start, null);

        var newParentResults = fixture.resolver.orgEnheter(
            OrgEnheterSearch.EMPTY_SEARCH,
            filter(UNDER_ANGITT_ORGENHET, "new-root", ALLE, AKTIVE)
        );
        assertThat(ids(newParentResults)).contains("moved", "descendant");
        assertThat(fixture.ids("old-root", AKTIVE)).doesNotContain("moved", "descendant");
    }

    @Test
    void aktive_enheter_under_historisk_kant_utelates_ogsaa_for_eksplisitte_ider() {
        var fixture = new Fixture();
        var yesterday = LocalDate.now().minusDays(1);
        var start = LocalDate.now().minusYears(1);
        fixture.enhet("old-root", start, null);
        fixture.enhet("new-root", start, null);
        fixture.enhet("moved", start, null);
        fixture.enhet("descendant", start, null);

        fixture.organisering("old-historical-edge", "moved", "old-root", start, yesterday);
        fixture.organisering("descendant-edge", "descendant", "moved", start, null);
        fixture.organisering("new-edge", "moved", "new-root", start, null);

        var explicitResults = fixture.resolver.orgEnheter(
            new OrgEnheterSearch(null, null, null, null, null, Set.of("moved", "descendant")),
            filter(UNDER_ANGITT_ORGENHET, "old-root", ALLE, AKTIVE)
        );
        assertAll(
            () -> assertThat(fixture.ids("old-root", AKTIVE, LOVNODE_ENHET)).doesNotContain("descendant"),
            () -> assertThat(explicitResults).allSatisfy(result -> assertThat(result.getCode()).isEqualTo(ResultCode.NOT_IN_FILTER))
        );
    }

    @Test
    void agresso_sok_filtrerer_etter_aktiv_organisering_og_returnerer_status_per_id() {
        var fixture = new Fixture();
        var yesterday = LocalDate.now().minusDays(1);
        var start = LocalDate.now().minusYears(1);
        fixture.enhet("old-root", start, null);
        fixture.enhet("new-root", start, null);
        fixture.enhet("moved", start, null);
        fixture.enhet("descendant", start, null);
        fixture.enhet("old-direct", start, null);

        fixture.organisering("old-historical-edge", "moved", "old-root", start, yesterday);
        fixture.organisering("descendant-edge", "descendant", "moved", start, null);
        fixture.organisering("new-edge", "moved", "new-root", start, null);
        fixture.organisering("old-direct-edge", "old-direct", "old-root", start, null);

        var search = new OrgEnheterSearch(
            Set.of("agresso-descendant", "agresso-old-direct"), null, null, null, null, null
        );
        var oldRootResults = fixture.resolver.orgEnheter(search, filter(UNDER_ANGITT_ORGENHET, "old-root", ALLE, AKTIVE));
        var oldRootResultsByAgressoId = oldRootResults.stream()
            .collect(Collectors.toMap(OrgEnhetResult::getAgressoId, Function.identity()));
        var newRootResults = fixture.resolver.orgEnheter(
            new OrgEnheterSearch(Set.of("agresso-descendant"), null, null, null, null, null),
            filter(UNDER_ANGITT_ORGENHET, "new-root", ALLE, AKTIVE)
        );

        assertAll(
            () -> assertThat(oldRootResultsByAgressoId.get("agresso-descendant").getCode()).isEqualTo(ResultCode.NOT_IN_FILTER),
            () -> assertThat(oldRootResultsByAgressoId.get("agresso-old-direct").getCode()).isEqualTo(ResultCode.OK),
            () -> assertThat(newRootResults).singleElement().satisfies(result -> {
                assertThat(result.getAgressoId()).isEqualTo("agresso-descendant");
                assertThat(result.getCode()).isEqualTo(ResultCode.OK);
            })
        );
    }

    @Test
    void paginert_sok_filtrerer_historisk_organisering_foer_paginering() {
        var fixture = new Fixture();
        var yesterday = LocalDate.now().minusDays(1);
        var start = LocalDate.now().minusYears(1);
        fixture.enhet("old-root", start, null);
        fixture.enhet("new-root", start, null);
        fixture.enhet("moved", start, null);
        fixture.enhet("descendant", start, null);
        fixture.enhet("old-direct", start, null);

        fixture.organisering("old-historical-edge", "moved", "old-root", start, yesterday);
        fixture.organisering("descendant-edge", "descendant", "moved", start, null);
        fixture.organisering("new-edge", "moved", "new-root", start, null);
        fixture.organisering("old-direct-edge", "old-direct", "old-root", start, null);

        var filter = filter(UNDER_ANGITT_ORGENHET, "old-root", ALLE, AKTIVE);
        var firstPage = fixture.resolver.orgEnheterPaginert(filter, new Pagineringsparametre(0, 1));
        var secondPage = fixture.resolver.orgEnheterPaginert(filter, new Pagineringsparametre(1, 1));

        assertAll(
            () -> assertThat(firstPage.getTotalCount()).isEqualTo(2),
            () -> assertThat(firstPage.getContent()).extracting("id").containsExactly("old-direct"),
            () -> assertThat(firstPage.getHasNextPage()).isTrue(),
            () -> assertThat(secondPage.getTotalCount()).isEqualTo(2),
            () -> assertThat(secondPage.getContent()).extracting("id").containsExactly("old-root"),
            () -> assertThat(secondPage.getHasNextPage()).isFalse(),
            () -> assertThat(List.of(firstPage, secondPage).stream().map(OrgEnhetSide::getContent).flatMap(List::stream)
                .map(orgEnhet -> orgEnhet.getId()).toList()).doesNotContain("descendant")
        );
    }

    @Test
    void alle_under_angitt_orgenhet_tar_med_historisk_organisering() {
        var fixture = new Fixture();
        var yesterday = LocalDate.now().minusDays(1);
        var start = LocalDate.now().minusYears(1);
        fixture.enhet("old-root", start, null);
        fixture.enhet("moved", start, null);
        fixture.organisering("old-historical-edge", "moved", "old-root", start, yesterday);

        var results = fixture.resolver.orgEnheter(
            OrgEnheterSearch.EMPTY_SEARCH,
            filter(UNDER_ANGITT_ORGENHET, "old-root", ALLE, PeriodiskUtvalgArg.ALLE)
        );

        assertThat(ids(results)).contains("moved");
    }

    private static OrgEnheterSearchFilter filter(
        OrgEnheterSearchFilter.OrganiseringSelection organisering,
        String parentId,
        OrgEnheterSearchFilter.LovnodeSelection lovnode,
        PeriodiskUtvalgArg period
    ) {
        return new OrgEnheterSearchFilter(lovnode, organisering, parentId, Set.of(), period, null);
    }

    private static List<String> ids(List<OrgEnhetResult> results) {
        return results.stream().map(OrgEnhetResult::getNomId).toList();
    }

    private static final class Fixture {
        private final OrgEnhetStateRepositoryImpl orgEnhetRepository = new OrgEnhetStateRepositoryImpl();
        private final OrganiseringStateRepositoryImpl organiseringRepository = new OrganiseringStateRepositoryImpl();
        private final OrgEnhetResolver resolver = new OrgEnhetResolver(
            orgEnhetRepository,
            NomGraphqlSupportInterface.dummyImplementation(),
            organiseringRepository
        );

        private void enhet(String id, LocalDate start, LocalDate end) {
            orgEnhetRepository.update(OrgEnhetState.builder()
                .fid("unit-" + id)
                .id(id)
                .navn("Enhet " + id)
                .agressoId("agresso-" + id)
                .gyldigFom(start)
                .gyldigTom(end)
                .nomNivaa(NomNivaa.LINJEENHET)
                .build());
        }

        private void organisering(String fid, String id, String parentId, LocalDate start, LocalDate end) {
            organiseringRepository.update(OrganiseringState.builder()
                .fid(fid)
                .id(id)
                .navn("Enhet " + id)
                .idOver(parentId)
                .navnOver("Enhet " + parentId)
                .gyldigFom(start)
                .gyldigTom(end)
                .build());
        }

        private List<String> ids(String parentId, PeriodiskUtvalgArg period) {
            return OrgEnhetResolverTest.ids(resolver.orgEnheter(
                OrgEnheterSearch.EMPTY_SEARCH,
                filter(UNDER_ANGITT_ORGENHET, parentId, ALLE, period)
            ));
        }

        private List<String> ids(String parentId, PeriodiskUtvalgArg period, OrgEnheterSearchFilter.LovnodeSelection lovnode) {
            return OrgEnhetResolverTest.ids(resolver.orgEnheter(
                OrgEnheterSearch.EMPTY_SEARCH,
                filter(UNDER_ANGITT_ORGENHET, parentId, lovnode, period)
            ));
        }
    }
}
