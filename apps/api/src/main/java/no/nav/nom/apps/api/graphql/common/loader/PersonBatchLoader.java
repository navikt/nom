package no.nav.nom.apps.api.graphql.common.loader;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.PDLBolkException;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.RessursNavn;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Slf4j
@Component
public class PersonBatchLoader {

    private final TokenRepository tokenRepository;
    private final AppProperties.Pdl pdlProperties;
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

    public PersonBatchLoader(TokenRepository tokenRepository, AppProperties appProperties, WebClient webClient) {
        this.tokenRepository = tokenRepository;
        this.pdlProperties = appProperties.pdl();
        this.graphQlClient = HttpGraphQlClient.builder(webClient)
                .url(pdlProperties.baseUrl() + "/graphql")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("Tema", pdlProperties.tema());
                })
                .build();
    }

    public <T> Map<T, DataFetcherResult<FolkeregisterPerson>> load(Collection<T> ressurser, Function<T, String> identResolver, BatchLoaderEnvironment batchLoaderEnvironment) {
        final var batch = getNameBatch(ressurser.stream().map(identResolver).collect(toSet()), batchLoaderEnvironment);
        return ressurser.stream()
                        .collect(
                                toUnmodifiableMap(
                                        Function.identity(),
                                        ressurs -> batch.getOrDefault(identResolver.apply(ressurs), DataFetcherResult.<FolkeregisterPerson>newResult().build())
                                )
                        );
    }

    @SneakyThrows
    public Map<String, DataFetcherResult<FolkeregisterPerson>> getNameBatch(Set<String> personidenter, BatchLoaderEnvironment batchLoaderEnvironment) {
        var response = graphQlClient.mutate()
                .headers((headers) -> headers.setBearerAuth(tokenRepository.getSystemAadToken(pdlProperties.aadClientId()).serialize()))
                .build()
                .document(PDL_BOLK_HENT_PERSON_QUERY)
                .variable("identer", personidenter)
                .execute()
                .block();

        if (response != null && !response.isValid()) {
            log.error("Feil i oppslag mot PDL: {}", response.getErrors());
            throw new PDLBolkException("Oppslag mot PDL feilet", response, ErrorCode.SERVER_ERROR);
        }

        final var data = Objects.requireNonNull(response)
                .field("hentPersonBolk")
                .toEntity(JsonNode.class); // TODO: Check if we can use toEntityList(JsonNode.class)

        return StreamSupport.stream(data.spliterator(), false)
                .collect(
                        toUnmodifiableMap(
                                o -> o.get("ident").asText(), // Fragile if the key comes back as empty string
                                o -> convert(o, (DataFetchingEnvironment) batchLoaderEnvironment.getKeyContexts().get(o.get("ident").asText()))
                        )
                );

    }

    private DataFetcherResult<FolkeregisterPerson> convert(JsonNode personResult, DataFetchingEnvironment env) {
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
        return convertToError(code, "PDL har ingen data på denne personidenten", env);
    }

    private DataFetcherResult<FolkeregisterPerson> convertToError(String code, String message, DataFetchingEnvironment env) {
        return DataFetcherResult.<FolkeregisterPerson>newResult()
                .error(ErrorCode.valueOf(code.toUpperCase()).construct(env, message))
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
