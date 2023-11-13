package no.nav.nom.apps.api.pdl;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.common.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PdlClient {

    private static final Logger logger = LoggerFactory.getLogger(PdlClient.class);
    private final static String PDL_HENT_PERSON_QUERY = // language=graphql
        """
        query($ident: ID!) {
            hentPerson(ident: $ident) {
                navn {
                    fornavn
                    mellomnavn
                    etternavn
                }
                kjoenn {
                    kjoenn
                }
                foedsel {
                   foedselsdato
                   foedselsaar
                }
           }
        }
        """;
    private final HttpGraphQlClient graphQlClient;

    @Autowired
    public PdlClient(
        AppProperties appProperties,
        OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager
    ) {
        final var pdlProperties = appProperties.clients().pdl();

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("pdl");
        this.graphQlClient = HttpGraphQlClient.builder(WebClient.builder().filter(oAuth2Filter))
            .url(pdlProperties.baseUrl() + "/graphql")
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("Tema", pdlProperties.tema());
                headers.add("behandlingsnummer", pdlProperties.behandlingsnummer());
            })
            .build();
    }

    public Optional<Person> hentPerson(String personIdent) {
        return graphQlClient.document(PDL_HENT_PERSON_QUERY)
            .variable("ident", personIdent)
            .execute()
            .filter(GraphQlResponse::isValid)
            .mapNotNull(res -> {
                if (!res.getErrors().isEmpty()) {
                    logger.warn("Error in lookup from PDL: {}", res.getErrors());
                    return null;
                }
                return res.field("hentPerson").toEntity(JsonNode.class);
            })
            .onErrorResume(throwable -> {
                logger.warn("Error in lookup from PDL", throwable);
                return Mono.empty();
            })
            .blockOptional()
            .map(this::convertToApiPerson);
    }

    private Person convertToApiPerson(JsonNode person) {
        final var navn = person.get("navn")
            .get(0);
        final var mellomnavn = navn.get("mellomnavn");
        final var kjonn = person.get("kjoenn")
            .get(0)
            .get("kjoenn")
            .asText();

        final var jsonFoedsel = person.get("foedsel").get(0);

        // foedselsdato might be missing, foedselsaar is always present.
        LocalDate foedselsdato = jsonFoedsel.hasNonNull("foedselsdato") ? LocalDate.parse(jsonFoedsel.get("foedselsdato").asText()) :
            LocalDate.of(jsonFoedsel.get("foedselsaar").asInt(), 1, 1);


        return Person.builder()
            .fornavn(navn.get("fornavn")
                .asText())
            .mellomnavn(mellomnavn.isTextual() ? mellomnavn.asText() : null)
            .etternavn(navn.get("etternavn")
                .asText())
            .kjonn(Kjonn.valueOf(kjonn))
            .foedselsdato(foedselsdato)
            .build();
    }
}
