package no.nav.nom.apps.api.graphql.request;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.repository.ordre.OrdreRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class OrdreResolver implements GraphQLQueryResolver {

    private final OrdreRepository ordreRepository;

    public DataFetcherResult<Ordre> ordre(UUID ordreId, DataFetchingEnvironment env) {
        return SecurityUtil.<Ordre>checkAuth(env).orElseGet(() -> {
            var set = ordreRepository.getById(ordreId);
            return build(ConverterUtils.convert(set));
        });
    }

    public DataFetcherResult<List<Ordre>> ordreBulk(List<UUID> requstSetIds, DataFetchingEnvironment env) {
        return SecurityUtil.<List<Ordre>>checkAuth(env).orElseGet(() -> {
            var sets = (isEmpty(requstSetIds) ? ordreRepository.findAll() : ordreRepository.findAllById(requstSetIds))
                    .stream()
                    .map(ConverterUtils::convert)
                    .toList();
            return build(sets);
        });
    }

    private <T> DataFetcherResult<T> build(T data) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .build();
    }
}
