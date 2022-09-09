package no.nav.nom.apps.api.graphql.common;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.dataloader.DataLoader;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@UtilityClass
public class LoaderUtils {

    public static <K, V> DataLoader<K, DataFetcherResult<V>> get(DataFetchingEnvironment env, String loaderName) {
        val registry = ((GraphQLKickstartContext) env.getContext()).getDataLoaderRegistry();
        return registry.getDataLoader(loaderName);
    }

    public static <K, V> CompletableFuture<DataFetcherResult<V>> get(DataFetchingEnvironment env, String loaderName, K key) {
        var loader = LoaderUtils.<K, V>get(env, loaderName);
        return loader.load(key, env);
    }

    public static <K, V, R> CompletableFuture<DataFetcherResult<R>> get(DataFetchingEnvironment env, String loaderName, K key, Function<V, R> transformer) {
        var result = LoaderUtils.<K, V>get(env, loaderName, key);
        return result.thenApply(r -> transform(transformer, r));
    }

    /**
     * {@link DataFetcherResult#transform(Consumer)} only supports transforming with the same response type
     */
    private static <V, R> DataFetcherResult<R> transform(Function<V, R> transformer, DataFetcherResult<V> r) {
        return DataFetcherResult.<R>newResult()
                .localContext(r.getLocalContext())
                .errors(r.getErrors())
                .data(Optional.ofNullable(r.getData()).map(transformer).orElse(null))
                .build();
    }

}
