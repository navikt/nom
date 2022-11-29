package no.nav.nom.apps.api.graphql.common.loader;

import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.exception.ADResponseException;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.ad.AdRessurs.AdStatus;
import no.nav.nom.contract.ad.AdRessurser;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
public class ActiveDirectoryRessursBatchLoader {

    private final TokenRepository tokenRepository;
    private final AppProperties.Proxy proxyProperties;
    private final RestTemplate restTemplate;

    public ActiveDirectoryRessursBatchLoader(TokenRepository tokenRepository, AppProperties appProperties, RestTemplate restTemplate) {
        this.tokenRepository = tokenRepository;
        this.proxyProperties = appProperties.proxy();
        this.restTemplate = restTemplate;
    }

    public <T> Map<Ressurs, DataFetcherResult<T>> load(Collection<Ressurs> ressurser, Function<AdRessurs, T> propertyResolver) {
        final var adRessurser = getAdRessurser(ressurser.stream().map(Ressurs::getNavIdent).collect(toSet()), propertyResolver);
        return ressurser.stream()
                .collect(toMap(Function.identity(), ressurs -> adRessurser.getOrDefault(ressurs.getNavIdent(), DataFetcherResult.<T>newResult().build())));
    }

    @SneakyThrows
    public <T> Map<String, DataFetcherResult<T>> getAdRessurser(Set<String> navIdenter, Function<AdRessurs, T> propertyResolver) {
        final var headers = new HttpHeaders();
        final var token = tokenRepository.getSystemAadToken(proxyProperties.aadClientId());
        headers.setBearerAuth(token.serialize());

        try {
            var result = restTemplate.exchange(
                    proxyProperties.baseUrl() + "/proxy/ad",
                    HttpMethod.POST,
                    new HttpEntity<>(navIdenter, headers),
                    AdRessurser.class
            );

            if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                return result.getBody().getRessurser().stream().collect(toMap(AdRessurs::getNavIdent, o -> convert(o, propertyResolver)));
            }
            throw new ADResponseException("Error in proxy query to AD code=" + result.getStatusCode(), result, ErrorCode.SERVER_ERROR);

        } catch (Exception exception) {
            log.error("Feil i oppslag mot AD i proxy", exception);
            return navIdenter.stream().collect(toMap(Function.identity(), p -> convertToError(ErrorCode.SERVER_ERROR, "Oppslag mot AD feilet")));
        }
    }

    private <T> DataFetcherResult<T> convert(AdRessurs ressurs, Function<AdRessurs, T> propertyResolver) {
        if (ressurs.getStatus() == AdStatus.OK) {
            return DataFetcherResult.<T>newResult().data(propertyResolver.apply(ressurs)).build();
        }
        log.warn("Manglende data fra AD", new GraphQLException("Manglende data fra AD for ident=" + ressurs.getNavIdent()));
        return convertToError(ErrorCode.NOT_FOUND, "AD har ingen data på denne navIdent");
    }

    private <T> DataFetcherResult<T> convertToError(ErrorCode errorCode, String message) {
        return DataFetcherResult.<T>newResult().error(errorCode.construct(message)).build();
    }

    public static GraphQLError handleADResponseException(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof ADResponseException exc) {
            return exc.getErrorCode().construct(environment, exc.getMessage());
        }
        return null;
    }
}
