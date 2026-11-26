package no.nav.nom.apps.testdata.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

final public class RekkefolgeValidator {
    private static final String FEILMELDING = "Rekkefølgen må inneholde alle rad-ID-ene nøyaktig én gang";

    private RekkefolgeValidator() {
    }

    public static <T> List<Long> kontrollerRekkefolge(
        List<Long> ids,
        List<T> eksisterendeRader,
        ToLongFunction<T> idExtractor
    ) {
        if (ids == null) {
            throw new IllegalArgumentException(FEILMELDING);
        }
        var rekkefolge = new ArrayList<>(ids);
        var unikeIder = new HashSet<>(rekkefolge);
        var eksisterendeIder = eksisterendeRader.stream()
            .mapToLong(idExtractor)
            .boxed()
            .collect(Collectors.toSet());
        if (unikeIder.contains(null) || unikeIder.size() != rekkefolge.size() || !unikeIder.equals(eksisterendeIder)) {
            throw new IllegalArgumentException(FEILMELDING);
        }
        return rekkefolge;
    }
}
