package no.nav.nom.apps.api.pdl;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PdlConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PdlConsumer.class);
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
    public PdlConsumer(
            AppProperties appProperties, TokenRepository tokenRepository, WebClient.Builder webClientBuilder
    ) {
        final var pdlProperties = appProperties.pdl();
        this.graphQlClient = HttpGraphQlClient.builder(webClientBuilder)
                .url(pdlProperties.baseUrl() + "/graphql")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("Tema", pdlProperties.tema());
                    headers.add("behandlingsnummer", pdlProperties.behandlingsnummer());
                })
                .webClient(builder -> builder.filter(PersonBatchLoader.setBearerAuthWith(tokenRepository, pdlProperties.aadClientId())))
                .build();
    }

    public Optional<Person> hentPerson(String personIdent) {
        return executePDLRequest(personIdent).filter(GraphQlResponse::isValid)
                .map(res -> res.field("hentPerson")
                        .toEntity(JsonNode.class))
                .onErrorResume(throwable -> {
                    logger.warn("Error in lookup from PDL", throwable);
                    return Mono.empty();
                })
                .blockOptional()
                .map(this::convertToApiPerson);
    }

    private Mono<ClientGraphQlResponse> executePDLRequest(String personident) {
        return graphQlClient.document(PDL_HENT_PERSON_QUERY)
                .variable("ident", personident)
                .execute();
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
