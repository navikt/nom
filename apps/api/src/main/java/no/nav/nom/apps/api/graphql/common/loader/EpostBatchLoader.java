package no.nav.nom.apps.api.graphql.common.loader;

import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.ad.AdRessurs.AdStatus;
import no.nav.nom.contract.ad.AdRessurser;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.MappedBatchLoaderWithContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpostBatchLoader implements MappedBatchLoaderWithContext<String, DataFetcherResult<String>> {

    public static String EPOST_DATALOADER = "EmailDataLoader";

    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    @Override
    public CompletionStage<Map<String, DataFetcherResult<String>>> load(Set<String> set, BatchLoaderEnvironment batchEnvironment) {
        return CompletableFuture.supplyAsync(() -> getEposterBatch(set, batchEnvironment));
    }

    @SneakyThrows
    public Map<String, DataFetcherResult<String>> getEposterBatch(Set<String> navIdenter, BatchLoaderEnvironment batchEnvironment) {
        var headers = new HttpHeaders();
        var token = tokenRepository.getAadToken(appProperties.getProxy().getAadClientId());
        headers.setBearerAuth(token);

        try {
            var result = restTemplate.exchange(appProperties.getProxy().getBaseUrl() + "/proxy/ad", HttpMethod.POST, new HttpEntity<>(navIdenter, headers), AdRessurser.class);

            if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                return result.getBody().getRessurser().stream()
                        .collect(Collectors.toMap(AdRessurs::getNavIdent, o -> convert(o, getEnv(batchEnvironment, o.getNavIdent()))));
            }
            throw new GraphQLException("Error in proxy query to AD code=" + result.getStatusCode());

        } catch (Exception exception) {
            log.error("Feil i oppslag mot AD i proxy", exception);
            return navIdenter.stream().collect(Collectors.toMap(p -> p,
                    p -> convertToError(ErrorCode.SERVER_ERROR, "Oppslag mot AD feilet", getEnv(batchEnvironment, p))));
        }
    }

    private DataFetcherResult<String> convert(AdRessurs ressurs, DataFetchingEnvironment env) {
        if (ressurs.getStatus() == AdStatus.OK) {
            return DataFetcherResult.<String>newResult()
                    .data(ressurs.getEmail())
                    .build();
        }
        log.warn("Manglende data fra AD", new GraphQLException("Manglende data fra AD for ident=" + ressurs.getNavIdent()));
        return convertToError(ErrorCode.NOT_FOUND, "AD har ingen data på denne navIdent", env);
    }

    private DataFetcherResult<String> convertToError(ErrorCode errorCode, String message, DataFetchingEnvironment env) {
        return DataFetcherResult.<String>newResult()
                .error(errorCode.construct(env, message))
                .build();
    }

    private DataFetchingEnvironment getEnv(BatchLoaderEnvironment batchLoaderEnvironment, String navIdent) {
        return (DataFetchingEnvironment) batchLoaderEnvironment.getKeyContexts().get(navIdent);
    }
}
