package no.nav.nom.apps.api.graphql.search;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.common.OrgEnhetStateBuilder;
import no.nav.nom.apps.api.common.RessursStateBuilder;
import no.nav.nom.apps.api.graphql.GraphQlTestBase;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GraphQlTest
@ActiveProfiles({"GraphQLTest", "test"})
@DisplayName("GraphQL - Search endepunkter")
public class SearchTest extends GraphQlTestBase {


    @BeforeEach
    void setup() {
        ressursStateRepository.clear();
        luceneService.clear();
    }

    @Test
    @DisplayName("Søk på navident")
    public void scenario12() {
        var r1 = new RessursStateBuilder(1).navident("testNavIdent").personident("testPersonIdent1").build();
        var r2 = new RessursStateBuilder(2).navident("testAnnenNav").personident("testPersonIdent2").build();

        addToRessursStateAndSearchIndex(List.of(r1, r2));

        final var response = gqlUtil.postForResource("search/scenario1.graphql");
        final var searchResults = response.path("search").entityList(Ressurs.class).get();

        assertEquals(1, searchResults.size());

        final var navidenter = searchResults.stream().map(Ressurs::getNavIdent).collect(Collectors.toList());
        assertThat(navidenter, hasItems(r1.navident()));
    }

    @Test
    @DisplayName("Søk på agressoId")
    public void scenario2() {
        var e1 = new OrgEnhetStateBuilder(1).navn("testid").uniqueAgressoId("unique-1").build();
        var e2 = new OrgEnhetStateBuilder(2).navn("testid2").uniqueAgressoId("unique-2").build();

        addToOrgenhetStateAndSearchIndex(List.of(e1, e2));

        final var response = gqlUtil.postForResource("search/scenario2.graphql");
        final var searchResults = response.path("search").entityList(OrgEnhet.class).get();
        final var agressoIds = searchResults.stream().map(OrgEnhet::getAgressoId).collect(Collectors.toList());

        assertEquals(2, agressoIds.size());
        assertTrue(agressoIds.contains(e1.agressoId()));
        assertTrue(agressoIds.contains(e2.agressoId()));
    }

    @Test
    @DisplayName("Søk på enhetsnavn")
    public void scenario3() {
        var mocknavn = "testenhet";
        var e1 = new OrgEnhetStateBuilder(1).navn(mocknavn).uniqueAgressoId("unique-1").build();

        addToOrgenhetStateAndSearchIndex(List.of(e1));


        final var response = gqlUtil.postForResource("search/scenario3.graphql");
        final var searchResults = response.path("search").entityList(OrgEnhet.class).get();
        final var enhetsnavn = searchResults.stream().map(OrgEnhet::getNavn).collect(Collectors.toList());

        assertTrue(enhetsnavn.contains(mocknavn));
    }

    @Test
    @DisplayName("Søk på term som resulterer i både enheter og ressurser")
    public void scenario4() {

        var r1 = new RessursStateBuilder(1).build();
        var r2 = new RessursStateBuilder(2).visningsnavn("test1", "etternavn").build();
        var e1 = new OrgEnhetStateBuilder(1).navn("testenhet").uniqueAgressoId("unique-1").build();

        addToRessursStateAndSearchIndex(List.of(r1, r2));
        addToOrgenhetStateAndSearchIndex(List.of(e1));

        final var response = gqlUtil.postForResource("search/scenario4.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        final var resultTypes = StreamSupport
            .stream(searchResults.spliterator(), false)
            .map(result -> result.get("__typename").asText())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertEquals(2, searchResults.size());
        assertEquals(1, resultTypes.getOrDefault("Ressurs", 0L));
        assertEquals(1, resultTypes.getOrDefault("OrgEnhet", 0L));
    }

    @Test
    @DisplayName("Søk på navn etter ressurs via lucene-wrapper")
    public void scenario51() {
        var r1 = new RessursStateBuilder(1).build();
        var r2 = new RessursStateBuilder(2).build();

        addToRessursStateAndSearchIndex(List.of(r1, r2));

        final var response = gqlUtil.postForResource("search/scenario5.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        assertTrue(searchResults.toString().contains("fornavn"));
    }

    @Test
    @DisplayName("Søk på navn etter orgenhet via lucene-wrapper")
    public void scenario61() {
        var r1 = new RessursStateBuilder(1).build();
        var r2 = new RessursStateBuilder(2).build();

        var e1 = new OrgEnhetStateBuilder(1).navn("NAV City").uniqueAgressoId("unique-1").build();
        var e2 = new OrgEnhetStateBuilder(2).navn("Not-to-find").uniqueAgressoId("unique-2").build();

        addToRessursStateAndSearchIndex(List.of(r1, r2));
        addToOrgenhetStateAndSearchIndex(List.of(e1, e2));

        final var response = gqlUtil.postForResource("search/scenario6.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        assertTrue(searchResults.toString().contains("NAV"));
    }

    @Test
    @DisplayName("Søk på navn etter ressurs via lucene-wrapper")
    public void scenario52() {
        var r1 = new RessursStateBuilder(1).navident("test").build();
        var r2 = new RessursStateBuilder(2).build();

        var e1 = new OrgEnhetStateBuilder(1).navn("fornavn").build();

        addToRessursStateAndSearchIndex(List.of(r1, r2));
        addToOrgenhetStateAndSearchIndex(List.of(e1));

        final var response = gqlUtil.postForResource("search/scenario5_1.graphql");
        final var searchResults = response.path("searchRessurs").entity(JsonNode.class).get();

        assertTrue(searchResults.toString().contains("test"));
        assertEquals(2, searchResults.size());
    }

    @Test
    @DisplayName("Søk etter orgenhet")
    public void scenario71() {
        var r1 = new RessursStateBuilder(1).build();
        var r2 = new RessursStateBuilder(2).build();

        var passedDate = LocalDate.now().minusMonths(1L);
        var e1 = new OrgEnhetStateBuilder(1).navn("test 3").gyldigTom(passedDate).uniqueAgressoId("unique-1").build();
        var e2 = new OrgEnhetStateBuilder(2).navn("test 4").uniqueAgressoId("unique-2").build();

        addToRessursStateAndSearchIndex(List.of(r1, r2));
        addToOrgenhetStateAndSearchIndex(List.of(e1, e2));

        final var allResponse = gqlUtil.postForResource(
            "search/scenario7.graphql",
            Map.of("filter", Map.of("orgnivSelection", "ALLE", "gyldigTomSelection", "ALLE"),
                "term", "test"
            )
        );
        final var all = allResponse.path("searchOrgEnhet").entity(JsonNode.class).get();

        assertEquals(2, all.size());
        assertTrue(all.toString().contains("test"));

        final var onlyActiveResponse = gqlUtil.postForResource(
            "search/scenario7.graphql",
            Map.of("filter", Map.of("orgnivSelection", "ALLE", "gyldigTomSelection", "IKKE_PASSERT"),
                "term", "test"
            )
        );
        final var onlyActive = onlyActiveResponse.path("searchOrgEnhet").entity(JsonNode.class).get();

        assertEquals(1, onlyActive.size());
    }

    @Test
    @DisplayName("Søk etter orgenhet orgniv filter")
    public void scenario71_orgniv() {
        var r1 = new RessursStateBuilder(1).visningsnavn("fornavn", "etternavn").build();

        var e1 = new OrgEnhetStateBuilder(1).navn("fornavnene").agressoOrgNivaa("ORGNIV2").uniqueAgressoId("unique-1").build();
        var e2 = new OrgEnhetStateBuilder(2).navn("fornavnet").uniqueAgressoId("unique-2").build();
        var e3 = new OrgEnhetStateBuilder(3).navn("fornavn").uniqueAgressoId("unique-3").build();

        addToRessursStateAndSearchIndex(List.of(r1));
        addToOrgenhetStateAndSearchIndex(List.of(e1, e2, e3));
        final var allResponse = gqlUtil.postForResource(
            "search/scenario7.graphql",
            Map.of("filter", Map.of("orgnivSelection", "ORGNIV_ORGENHET", "gyldigTomSelection", "ALLE"),
                "term", "forn"
            )
        );
        final var all = allResponse.path("searchOrgEnhet").entity(JsonNode.class).get();

        assertTrue(all.toString().contains("fornavnet"));
        assertFalse(all.toString().contains("fornavnene"));
        assertEquals(2, all.size());
    }

    private void addToRessursStateAndSearchIndex(List<RessursState> states) {
        states.forEach(state -> {
            ressursStateRepository.update(state);
            luceneService.invalidateRessurs(state.navident());
        });
        luceneService.addInvalidatedEntitiesToIndex();
    }

    private void addToOrgenhetStateAndSearchIndex(List<OrgEnhetState> states) {
        states.forEach(state -> {
            orgEnhetStateRepository.update(state);
            luceneService.invalidateOrgenhet(state.id());
        });
        luceneService.addInvalidatedEntitiesToIndex();
    }

}