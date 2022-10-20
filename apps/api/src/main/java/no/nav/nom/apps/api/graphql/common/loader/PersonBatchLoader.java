package no.nav.nom.apps.api.graphql.common.loader;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.PDLBolkException;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.RessursNavn;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class PersonBatchLoader {

    private final HttpGraphQlClient graphQlClient;

    private final static String PDL_BOLK_HENT_PERSON_QUERY = // language=graphql
            """
                query($identer: [ID!]!) {
                    hentPersonBolk(identer: $identer) {
                        ident
                        person {
                            navn(historikk: false) {
                                fornavn
                                etternavn
                                mellomnavn
                            }
                        }
                        code
                    }
                }
                """;

    public PersonBatchLoader(TokenRepository tokenRepository, AppProperties appProperties, WebClient.Builder webClientBuilder) {
        var pdlProperties = appProperties.pdl();
        this.graphQlClient = HttpGraphQlClient.builder(webClientBuilder)
                .url(pdlProperties.baseUrl() + "/graphql")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("Tema", pdlProperties.tema());
                })
                .webClient(builder -> builder.filter(setBearerAuthWith(tokenRepository, pdlProperties.aadClientId())))
                .build();
    }

    private  ExchangeFilterFunction setBearerAuthWith(TokenRepository tokenRepository, String clientId) {
        // Utility outbound exchange filter on webclient to set the authorization header
        return ExchangeFilterFunction.ofRequestProcessor(
                request -> Mono.just(
                        ClientRequest.from(request)
                                .headers(headers -> headers.setBearerAuth(tokenRepository.getSystemAadToken(clientId).serialize()))
                                .build()
                )
        );
    }

    public <T> Mono<Map<T, DataFetcherResult<FolkeregisterPerson>>> loadMany(Collection<T> ressurser, Function<T, String> identResolver) {
        // TODO: Consider reintroducing BatchLoaderEnvironment parameter if the DataFetchingEnvironment is added to the keycontext, https://github.com/spring-projects/spring-graphql/issues/517
        return getPDLBatch(ressurser.stream().collect(toMap(identResolver, Function.identity())));
    }

    public <T> Mono<DataFetcherResult<FolkeregisterPerson>> loadSingle(T ressurs, Function<T, String> identResolver) {
        return getPDLBatch(Map.of(identResolver.apply(ressurs), ressurs)).map(map -> map.get(ressurs));
    }

    public <T> Mono<Map<T, DataFetcherResult<FolkeregisterPerson>>> getPDLBatch(Map<String, T> ressurser) {
        return executePDLRequest(ressurser.keySet())
                .map(response -> {
                    if (response != null && !response.isValid()) {
                        log.error("Feil i oppslag mot PDL: {}", response.getErrors());
                        throw new PDLBolkException("Oppslag mot PDL feilet", response, ErrorCode.SERVER_ERROR);
                    }

                    return requireNonNull(response)
                            .field("hentPersonBolk")
                            .toEntity(JsonNode.class);
                })
                .flatMapIterable(Function.identity())
                .map(person -> Map.entry(person.get("ident").asText(), convert(person)))
                .reduce(new HashMap<>(), (results, entry) -> {
                    results.putIfAbsent(ressurser.get(entry.getKey()), entry.getValue());
                    return results;
                });
    }

    private Mono<ClientGraphQlResponse> executePDLRequest(Collection<String> personidenter) {
        return graphQlClient
                .document(PDL_BOLK_HENT_PERSON_QUERY)
                .variable("identer", personidenter)
                .execute();
    }

    private DataFetcherResult<FolkeregisterPerson> convert(JsonNode personResult) {
        final var code = personResult.get("code").asText();
        if ("ok".equals(code)) {
            final var navn = personResult.get("person").get("navn").get(0);
            final var mellomnavn = navn.get("mellomnavn");

            return DataFetcherResult.<FolkeregisterPerson>newResult()
                    .data(FolkeregisterPerson.builder()
                            .navn(
                                    RessursNavn.builder()
                                            .fornavn(navn.get("fornavn").asText())
                                            .mellomnavn(mellomnavn.isTextual() ? mellomnavn.asText() : null)
                                            .etternavn(navn.get("etternavn").asText())
                                            .build()
                            )
                            .build()
                    )
                    .build();
        }
        log.warn("Manglende data fra PDL", new GraphQLException("Manglende data fra PDL med kode: " + code));
        return convertToError(code, "PDL har ingen data på denne personidenten");
    }

    private DataFetcherResult<FolkeregisterPerson> convertToError(String code, String message) {
        return DataFetcherResult.<FolkeregisterPerson>newResult()
                .error(ErrorCode.valueOf(code.toUpperCase()).construct(message))
                .build();
    }

    public static List<GraphQLError> handlePDLBolkException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof PDLBolkException exception) {
            return exception.getErrors().stream()
                    .map(error -> exception.getErrorCode().construct(environment, exception.getMessage()))
                    .toList();
        }
        return null;
    }
}
