package no.nav.nom.apps.api.graphql.search;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.graphql.GraphQLTestBase;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@DisplayName("GraphQL.Search")
public class SearchTest extends GraphQLTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Test
    @SneakyThrows
    @DisplayName("Søk på navident")
    public void scenario1() {
        saveHendelser(createRessursHendelse("1"), createRessursHendelse("2"));
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");

        val response = postForResource("search/scenario1.graphql", false);
        val searchResults = response.getList("$.data.search", Ressurs.class);

        assertThat(searchResults, hasSize(2));

        val navidenter = searchResults.stream().map(Ressurs::getNavIdent).collect(Collectors.toList());
        assertThat(navidenter, hasItems("testNavIdent1", "testNavIdent2"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Søk på agressoId")
    public void scenario2() {
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("search/scenario2.graphql", false);
        val searchResults = response.getList("$.data.search", Organisasjonsenhet.class);
        val agressoIds = searchResults.stream().map(Organisasjonsenhet::getAgressoId).collect(Collectors.toList());

        assertThat(searchResults.size(), greaterThan(1));
        assertThat(agressoIds, hasItem("testId1"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Søk på enhetsnavn")
    public void scenario3() {
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("search/scenario3.graphql", false);
        val searchResults = response.getList("$.data.search", Organisasjonsenhet.class);
        val enhetsnavn = searchResults.stream().map(Organisasjonsenhet::getNavn).collect(Collectors.toList());

        assertThat(enhetsnavn, hasItem("testNavn"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Søk på term som resulterer i både enheter og ressurser")
    public void scenario4() {
        saveHendelser(createRessursHendelse("1"), createRessursHendelse("2"));
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent1", "testPersonIdent2");
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("search/scenario4.graphql", false);
        val searchResults = response.get("$.data.search", JsonNode.class);

        val resultTypes = StreamSupport
                .stream(searchResults.spliterator(), false)
                .map(result -> result.get("__typename").asText())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertThat(resultTypes.getOrDefault("Ressurs", 0L), greaterThan(0L));
        assertThat(resultTypes.getOrDefault("Organisasjonsenhet", 0L), greaterThan(0L));
    }
}

