package no.nav.nom.apps.api.graphql.search;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.common.OrgEnhetStateBuilder;
import no.nav.nom.apps.api.common.RessursStateBuilder;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.graphql.GraphQLExtendedIntegrationTestBase;
import no.nav.nom.apps.api.graphql.OldGraphQLTestBase;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@DisplayName("GraphQL.Search")
public class SearchTest extends GraphQLExtendedIntegrationTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private LuceneService luceneService;

    @Autowired
    DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @BeforeEach
    void clearLucene() {
        luceneService.clear();
    }

    @Test
    @DisplayName("Søk på navident")
    public void scenario12() {
        var r1 = new RessursStateBuilder(1).navident("testNavIdent").personident("testPersonIdent1").build();
        var r2 = new RessursStateBuilder(2).navident("testAnnenNav").personident("testPersonIdent2").build();

        List.of(r1,r2).forEach(kafkaUtils::createRessursHendelseEventSource);

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        luceneService.addInvalidatedEntitiesToIndex();

        final var response = postForResource("search/scenario1.graphql");
        final var searchResults = response.path("search").entityList(Ressurs.class).get();

        assertThat(searchResults, hasSize(1));

        final var navidenter = searchResults.stream().map(Ressurs::getNavIdent).collect(Collectors.toList());
        assertThat(navidenter, hasItems(r1.navident()));
    }

    @Test
    @DisplayName("Søk på agressoId")
    public void scenario2() {
        var e1 = new OrgEnhetStateBuilder(1).navn("testid").uniqueAgressoId(Optional.of("unique-1")).build();
        var e2 = new OrgEnhetStateBuilder(2).navn("testid2").uniqueAgressoId(Optional.of("unique-2")).build();
        List.of(e1, e2).forEach(kafkaUtils::createOrgenhetHendelseEventSource);

        luceneService.addInvalidatedEntitiesToIndex();

        final var response = postForResource("search/scenario2.graphql");
        final var searchResults = response.path("search").entityList(OrgEnhet.class).get();
        final var agressoIds = searchResults.stream().map(OrgEnhet::getAgressoId).collect(Collectors.toList());

        assertThat(searchResults.size(), greaterThan(1));
        assertThat(agressoIds, hasItem(e1.agressoId().get()));
        assertThat(agressoIds, hasItem(e2.agressoId().get()));

    }

    @Test
    @DisplayName("Søk på enhetsnavn")
    public void scenario3() {

        var mocknavn = "testenhet";
        var e1 = new OrgEnhetStateBuilder(1).navn(mocknavn).uniqueAgressoId(Optional.of("unique-1")).build();
        kafkaUtils.createOrgenhetHendelseEventSource(e1);
        luceneService.addInvalidatedEntitiesToIndex();

        final var response = postForResource("search/scenario3.graphql");
        final var searchResults = response.path("search").entityList(OrgEnhet.class).get();
        final var enhetsnavn = searchResults.stream().map(OrgEnhet::getNavn).collect(Collectors.toList());

        assertThat(enhetsnavn, hasItem(mocknavn));
    }

    @Test
    @DisplayName("Søk på term som resulterer i både enheter og ressurser")
    public void scenario4() {

        var r1 = new RessursStateBuilder(1).build();
        var r2 = new RessursStateBuilder(2).visningsnavn("test1", "etternavn").build();
        var e1 = new OrgEnhetStateBuilder(1).navn("testenhet").uniqueAgressoId(Optional.of("unique-1")).build();

        List.of(r1,r2).forEach(kafkaUtils::createRessursHendelseEventSource);
        List.of(e1).forEach(kafkaUtils::createOrgenhetHendelseEventSource);

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        luceneService.addInvalidatedEntitiesToIndex();

        final var response = postForResource("search/scenario4.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        final var resultTypes = StreamSupport
            .stream(searchResults.spliterator(), false)
            .map(result -> result.get("__typename").asText())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertThat(resultTypes.getOrDefault("Ressurs", 0L), greaterThan(0L));
        assertThat(resultTypes.getOrDefault("OrgEnhet", 0L), greaterThan(0L));
    }

    @Test
    @DisplayName("Søk på navn etter ressurs via lucene-wrapper")
    public void scenario51() {
        kafkaUtils.createRessursHendelseEventSource("1");
        kafkaUtils.createRessursHendelseEventSource("2");

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");
        dvhRestConsumer.loadRessurserOgOrgEnheter();
        luceneService.addInvalidatedEntitiesToIndex();

        final var response = postForResource("search/scenario5.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        assertThat(searchResults.toString(), containsString("fornavn"));
    }

    @Test
    @DisplayName("Søk på navn etter orgenhet via lucene-wrapper")
    public void scenario61() {
        kafkaUtils.createRessursHendelseEventSource("1");
        kafkaUtils.createRessursHendelseEventSource("2");

        kafkaUtils.createOrgenhetHendelseEventSource(new OrgEnhetStateBuilder(1).navn("NAV City").uniqueAgressoId(Optional.of("unique-1")).build());
        kafkaUtils.createOrgenhetHendelseEventSource(new OrgEnhetStateBuilder(2).navn("Not-to-find").uniqueAgressoId(Optional.of("unique-2")).build());

        luceneService.addInvalidatedEntitiesToIndex();
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        final var response = postForResource("search/scenario6.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        assertThat(searchResults.toString(), containsString("NAV"));
    }

    @Test
    @DisplayName("Søk på navn etter ressurs via lucene-wrapper")
    public void scenario52() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("fornavn").build();

        kafkaUtils.createRessursHendelseEventSource("1");
        kafkaUtils.createRessursHendelseEventSource("2");

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();
        luceneService.addInvalidatedEntitiesToIndex();
        luceneService.refreshOrgenhet();

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        final var response = postForResource("search/scenario5_1.graphql");
        final var searchResults = response.path("searchRessurs").entity(JsonNode.class).get();

        assertThat(searchResults.toString(), containsString("test"));
        assertThat(searchResults.size(), equalTo(2));
    }

    @Test
    @DisplayName("Søk etter orgenhet")
    public void scenario71() {
        var passedDate = LocalDate.now().minusMonths(1L);

        kafkaUtils.createRessursHendelseEventSource("1");
        kafkaUtils.createRessursHendelseEventSource("2");
        kafkaUtils.createOrgenhetHendelseEventSource(new OrgEnhetStateBuilder(3).navn("test 3").gyldigTom(Optional.of(passedDate)).uniqueAgressoId(Optional.of("unique-1")).build());
        kafkaUtils.createOrgenhetHendelseEventSource(new OrgEnhetStateBuilder(4).navn("test 4").uniqueAgressoId(Optional.of("unique-2")).build());

        dvhRestConsumer.loadRessurserOgOrgEnheter();
        luceneService.addInvalidatedEntitiesToIndex();

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        final var allResponse = postForResource(
            "search/scenario7.graphql",
            Map.of("filter", Map.of("orgnivSelection", "ALLE", "gyldigTomSelection", "ALLE"),
                "term", "test"
            )
        );
        final var all = allResponse.path("searchOrgEnhet").entity(JsonNode.class).get();

        assertThat(all.size(), equalTo(2));
        assertThat(all.toString(), containsString("test"));

        final var onlyActiveResponse = postForResource(
            "search/scenario7.graphql",
            Map.of("filter", Map.of("orgnivSelection", "ALLE", "gyldigTomSelection", "IKKE_PASSERT"),
                "term", "test"
            )
        );
        final var onlyActive = onlyActiveResponse.path("searchOrgEnhet").entity(JsonNode.class).get();
        assertThat(onlyActive.size(), equalTo(1));
    }

    @Test
    @DisplayName("Søk etter orgenhet orgniv filter")
    public void scenario71_orgniv() {
        var r1 = new RessursStateBuilder(1).visningsnavn("fornavn", "etternavn").build();

        var e1 = new OrgEnhetStateBuilder(1).navn("fornavnene").agressoOrgNivaa(Optional.of("ORGNIV2")).uniqueAgressoId(Optional.of("unique-1")).build();
        var e2 = new OrgEnhetStateBuilder(2).navn("fornavnet").uniqueAgressoId(Optional.of("unique-2")).build();
        var e3 = new OrgEnhetStateBuilder(3).navn("fornavn").uniqueAgressoId(Optional.of("unique-3")).build();

        List.of(e1, e2, e3).forEach(kafkaUtils::createOrgenhetHendelseEventSource);
        List.of(r1).forEach(kafkaUtils::createRessursHendelseEventSource);

        luceneService.addInvalidatedEntitiesToIndex();

        final var allResponse = postForResource(
            "search/scenario7.graphql",
            Map.of("filter", Map.of("orgnivSelection", "ORGNIV_ORGENHET", "gyldigTomSelection", "ALLE"),
                "term", "forn"
            )
        );
        final var all = allResponse.path("searchOrgEnhet").entity(JsonNode.class).get();

        assertThat(all.toString(), containsString("fornavnet"));
        assertThat(all.toString(), not(containsString("fornavnene")));
        assertThat(all.size(), equalTo(2));
    }

}