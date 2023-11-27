package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class NomApiClient {

    private final WebClient nomApiAuthorizedWebClient;
    private final String getOrgenhetGraphQLQuery = readResourceFile("graphql/nom/orgenhet_query.graphql");
    private final String getRessursGraplQlQuery = readResourceFile("graphql/nom/get_ressurs_query.graphql");

    private final Retry defaultRetry = Retry
        .backoff(3, Duration.ofMillis(1))
        .jitter(0.75)
        .filter(throwable -> {
            if (throwable instanceof WebClientResponseException) {
                return ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
            }
            return false;
        })
        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
            Throwable throwable = retrySignal.failure();
            if (throwable instanceof WebClientResponseException) {
                throw (WebClientResponseException) throwable;
            }
            throw new RuntimeException(throwable);
        });

    public NomApiClient(WebClient nomApiAuthorizedWebClient) {
        this.nomApiAuthorizedWebClient = nomApiAuthorizedWebClient;
    }


    public Optional<RessursWithSektor> getRessurs(String navident) {
        return nomApiAuthorizedWebClient.post()
            .uri("/graphql")
            .bodyValue(
                new GraphqlQuery<>(
                    getRessursGraplQlQuery,
                    new RessursQueryVars(navident),
                    "getRessurs"
                )
            )
            .retrieve()
            .bodyToMono(RessursGraphQLResponse.class)
            .retryWhen(defaultRetry)
            .doOnSuccess(response -> {
                    if (response.data.isEmpty()) {
                        response.errors.ifPresent(graphQLErrors -> {
                            throw new NomApiGraphQLException(graphQLErrors, getRessursGraplQlQuery);
                        });
                    }
                }
            )
            .block()
            .data().map(ressursResponseData -> ressursResponseData.ressurs);
    }

    public Optional<OrgEnhetWithRemedyId> getOrgEnhetWithRemedyId(String orgenhetId) {

        return nomApiAuthorizedWebClient.post()
            .uri("/graphql")
            .bodyValue(
                new GraphqlQuery<>(
                    getOrgenhetGraphQLQuery,
                    new OrgenhetQueryVars(orgenhetId),
                    "getOrgEnhet"
                )
            )
            .retrieve()
            .bodyToMono(OrgenhetGraphQLResponse.class)
            .retryWhen(defaultRetry)
            .doOnSuccess(response -> {
                    if (response.data.isEmpty()) {
                        response.errors.ifPresent(graphQLErrors -> {
                            throw new NomApiGraphQLException(graphQLErrors, getOrgenhetGraphQLQuery);
                        });
                    }
                }
            )
            .block()
            .data().map(orgEnhetResponseData -> orgEnhetResponseData.orgEnhet);
    }

    record GraphqlQuery<T>(
        String query,
        T variables,
        String operationName
    ) {
    }

    record OrgenhetQueryVars(
        String id
    ) {}

    record RessursQueryVars(
        String navident
    ) {}

    record OrgenhetGraphQLResponse(
        Optional<OrgEnhetResponseData> data,
        Optional<List<GraphQLError>> errors
    ) {}

    record OrgEnhetResponseData(
        OrgEnhetWithRemedyId orgEnhet
    ) {}

    record RessursGraphQLResponse(
        Optional<RessursResponseData> data,
        Optional<List<GraphQLError>> errors
    ) {}

    record RessursResponseData(
        RessursWithSektor ressurs
    ) {}

    private static String readResourceFile(String resourceFilePath) {
        Resource resource = new ClassPathResource(resourceFilePath);
        try {
            Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8);
            return FileCopyUtils.copyToString(reader);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
