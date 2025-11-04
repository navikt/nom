package no.nav.nom.libs.utils.common.util;

import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class StreamUtils {

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static <T> Stream<T> safeStream(Iterable<T> iterable) {
        return iterable == null ? Stream.empty() : StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> T find(Iterable<T> objects, Predicate<T> filter) {
        return safeStream(objects).filter(filter).findFirst().orElseThrow(() -> new IllegalArgumentException("could not find item"));
    }

    public static <K, V> Map<K, V> toMap(Iterable<V> from, Function<? super V, K> keyExtractor) {
        return safeStream(from).collect(Collectors.toMap(keyExtractor, Function.identity()));
    }

    public <T, K, V> Collector<T, ?, LinkedHashMap<K, V>> collectToOrderedMap(
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends V> valueMapper
    ) {
        return Collectors.toMap(
            keyMapper,
            valueMapper,
            (exsisting, replacement) -> exsisting,
            LinkedHashMap::new
        );
    }
}
