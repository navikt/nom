package no.nav.nom.apps.api.graphql.search;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.graphql.GraphQLTestBase;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

@DisplayName("GraphQL.Search")
public class SearchTest extends GraphQLTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private LuceneService luceneService;

    @Test
    @DisplayName("Søk på navident")
    public void scenario12() {
        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        final var response = postForResource("search/scenario1.graphql");
        final var searchResults = response.path("search").entityList(Ressurs.class).get();

        assertThat(searchResults, hasSize(2));

        final var navidenter = searchResults.stream().map(Ressurs::getNavIdent).collect(Collectors.toList());
        assertThat(navidenter, hasItems("testNavIdent1", "testNavIdent2"));
    }

    @Test
    @DisplayName("Søk på agressoId")
    public void scenario2() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testid").build();
        final var mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testid2").build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        final var response = postForResource("search/scenario2.graphql");
        final var searchResults = response.path("search").entityList(OrgEnhet.class).get();
        final var agressoIds = searchResults.stream().map(OrgEnhet::getAgressoId).collect(Collectors.toList());

        assertThat(searchResults.size(), greaterThan(1));
        assertThat(agressoIds, hasItem(mockedEnhet.getORGENHET_KODE()));
        assertThat(agressoIds, hasItem(mockedEnhet2.getORGENHET_KODE()));

    }

    @Test
    @DisplayName("Søk på enhetsnavn")
    public void scenario3() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("testnavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        final var response = postForResource("search/scenario3.graphql");
        final var searchResults = response.path("search").entityList(OrgEnhet.class).get();
        final var enhetsnavn = searchResults.stream().map(OrgEnhet::getNavn).collect(Collectors.toList());

        assertThat(enhetsnavn, hasItem(mockedEnhet.getORGENHET_NAVN()));
    }

    @Test
    @DisplayName("Søk på term som resulterer i både enheter og ressurser")
    public void scenario4() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("testnavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);

        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");
        dvhRestConsumer.loadRessurserOgOrgEnheter();

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
        luceneEnabled(true);
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("testnavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);

        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        luceneService.addInvalidatedEntitiesToIndex();

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        final var response = postForResource("search/scenario5.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        assertThat(searchResults.toString(), containsString("fornavn"));
    }

    @Test
    @DisplayName("Søk på navn etter orgenhet via lucene-wrapper")
    public void scenario61() {
        luceneEnabled(true);

        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("NAV City").build();
        final var mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("Not-to-find").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        luceneService.addInvalidatedEntitiesToIndex();
        luceneService.refreshOrgenhet();

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        final var response = postForResource("search/scenario6.graphql");
        final var searchResults = response.path("search").entity(JsonNode.class).get();

        assertThat(searchResults.toString(), containsString("NAV"));
    }

    @Test
    @DisplayName("Søk på navn etter ressurs via lucene-wrapper")
    public void scenario52() {
        luceneEnabled(true);
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("fornavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);

        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        luceneService.addInvalidatedEntitiesToIndex();
        luceneService.refreshOrgenhet();

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        final var response = postForResource("search/scenario5_1.graphql");
        final var searchResults = response.path("searchRessurs").entity(JsonNode.class).get();

        assertThat(searchResults.toString(), containsString("test"));
        assertThat(searchResults.size(),equalTo(2));
    }
}

