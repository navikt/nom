package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

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
    private final String getOrgenhetGraphQLQuery = readGetOrgenhetGraphQLQueryResourceFile();

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

    public Optional<OrgEnhetWithRemedyId> getOrgEnhetWithRemedyId(String orgenhetId) {

        OrgEnhetResponse orgEnhetResponse = nomApiAuthorizedWebClient.post()
            .uri("/graphql")
            .bodyValue(
                new GetOrgenhetQuery(
                    getOrgenhetGraphQLQuery,
                    new GetOrgenhetQuery.OrgenhetQueryVars(orgenhetId),
                    "getOrgEnhet"
                )
            )
            .retrieve()
            .bodyToMono(OrgEnhetResponse.class)
            .retryWhen(defaultRetry)
            .doOnSuccess( response -> response.errors.ifPresent(graphQLErrors -> {
                throw new RuntimeException(
                    String.join("\n", graphQLErrors.stream().map(graphQLError -> graphQLError.message).toList())
                );
            })
            )
            .block();

        return Optional.ofNullable(orgEnhetResponse.data().orgEnhet);

    }

    record GetOrgenhetQuery(
        String query,
        OrgenhetQueryVars variables,
        String operationName
    ) {

        record OrgenhetQueryVars(
            String id
        ) {}
    }

    record OrgEnhetResponse(
        OrgEnhetResponseData data,
        Optional<List<GraphQLError>> errors
    ) {
        record OrgEnhetResponseData(
            OrgEnhetWithRemedyId orgEnhet
        ) {}

        record GraphQLError (
            String message
        ) {}
    }

    private static String readGetOrgenhetGraphQLQueryResourceFile() {
        Resource resource = new ClassPathResource("graphql/nom/orgenhet_query.graphql");
        try {
            Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8);
            return FileCopyUtils.copyToString(reader);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
