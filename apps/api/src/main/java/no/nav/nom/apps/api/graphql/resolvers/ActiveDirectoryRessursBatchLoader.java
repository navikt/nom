package no.nav.nom.apps.api.graphql.resolvers;

import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.ad.AdRessurs.AdStatus;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static no.nav.nom.apps.api.clients.ad.AdClient.AD_MAX_BATCH_SIZE;
import static no.nav.nom.apps.api.clients.ad.AdClient.listToBatches;

@Slf4j
@Component
public class ActiveDirectoryRessursBatchLoader {


    private final AdClient adClient;

    public ActiveDirectoryRessursBatchLoader(AdClient adClient) {
        this.adClient = adClient;
    }

    public <T> Map<Ressurs, DataFetcherResult<T>> load(List<Ressurs> ressurser, Function<AdRessurs, T> propertyResolver) {
        final var adRessurser = listToBatches(ressurser, AD_MAX_BATCH_SIZE).stream() // Split into MAX_BATCH_SIZE lists
            .map(batch -> getAdRessurser(batch.stream().map(Ressurs::getNavident).collect(toSet()), propertyResolver)) // Get AD ressurser from nom-proxy
            .flatMap(batchResult -> batchResult.entrySet().stream()) // Convert all Maps to a big stream of EntrySet
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)); // Collect all entries to a Map

        return ressurser.stream()
                .collect(toMap(Function.identity(), ressurs -> adRessurser.getOrDefault(ressurs.getNavident(), DataFetcherResult.<T>newResult().build())));
    }

    @SneakyThrows
    public <T> Map<String, DataFetcherResult<T>> getAdRessurser(Set<String> navIdenter, Function<AdRessurs, T> propertyResolver) {
        try {
            var result = adClient.getAdRessurser(navIdenter);

            return result.getRessurser().stream().collect(toMap(AdRessurs::getNavIdent, o -> convert(o, propertyResolver)));

        } catch (Exception exception) {
            log.error("Feil i oppslag mot AD i proxy", exception);
            return navIdenter.stream().collect(toMap(Function.identity(), p -> convertToError(ErrorCode.SERVER_ERROR, "Oppslag mot AD feilet")));
        }
    }


    private <T> DataFetcherResult<T> convert(AdRessurs ressurs, Function<AdRessurs, T> propertyResolver) {
        if (ressurs.getStatus() == AdStatus.OK) {
            return DataFetcherResult.<T>newResult().data(propertyResolver.apply(ressurs)).build();
        }
        log.debug("Manglende data fra AD", new GraphQLException("Manglende data fra AD for ident=" + ressurs.getNavIdent()));
        return convertToError(ErrorCode.NOT_FOUND, "AD har ingen data på nav-ident: " + ressurs.getNavIdent());
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
