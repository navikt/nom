package no.nav.nom.apps.api.graphql.metrics;

import graphql.schema.DataFetchingEnvironment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.querystats.GqlQueryStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class GraphQLFieldConsumptionCounter {

    private final static String METRICS_COUNTER_NAME = "graphql_query_field_counter";
    private final static Logger logger = LoggerFactory.getLogger(GraphQLFieldConsumptionCounter.class);

    private final MeterRegistry meterRegistry;
    private final SecurityUtil securityUtil;
    private final GqlQueryStatsService gqlQueryStatsService;
    private final Map<String, Counter> counterByNameAndTagValues = new HashMap<>();

    @Autowired
    public GraphQLFieldConsumptionCounter(MeterRegistry meterRegistry, SecurityUtil securityUtil, GqlQueryStatsService gqlQueryStatsService) {
        this.meterRegistry = meterRegistry;
        this.securityUtil = securityUtil;
        this.gqlQueryStatsService = gqlQueryStatsService;
    }

    @SuppressWarnings("unchecked")
    public void count(DataFetchingEnvironment env) {
        final var consumer = securityUtil.getAzpName().orElse("unknown");
        final var operation = getOperationName(env);
        final var queryRoot = env.getField().getName();

        List<FieldPathEntry> fieldPathList = new ArrayList<>();

        // Søke argumenter parameter navn
        env.getArguments().forEach((argument, value) -> {
            if (!(value instanceof Map<?, ?>)) {
                return;
            }
            var path = String.format("%s.argument.%s", queryRoot, argument);
            try {
                ((Map<String, Object>) value).forEach((paramName, _) -> {
                    fieldPathList.add(new FieldPathEntry(paramName, path));
                    registerValuesToMetrics(paramName, path, consumer, operation);
                });
            } catch (ClassCastException ignored) {}
        });

        registerValuesToMetrics(queryRoot, queryRoot, consumer, operation); // register query root

        env.getSelectionSet().getFields().forEach(field -> {
            final var currentPath = String.format("%s.%s", queryRoot, field.getQualifiedName().replace("/", "."));
            fieldPathList.add(new FieldPathEntry(field.getName(), currentPath));
            registerValuesToMetrics(field.getName(), currentPath, consumer, operation); // register fields beneath query root
        });
        gqlQueryStatsService.registerFieldUsage(fieldPathList, operation, consumer);
    }

    private static String getOperationName(DataFetchingEnvironment env) {
        var operationName =  env.getOperationDefinition().getName(); // operation name is the query alias, which may be null
        return isEmpty(operationName) ? "unknown" : operationName;
    }

    private void registerValuesToMetrics(String field, String path, String consumer, String operation) {
        Map<String, String> tagsAndValues = new LinkedHashMap<>();

        tagsAndValues.put("field", field);
        tagsAndValues.put("path", path);
        tagsAndValues.put("consumer", consumer);
        tagsAndValues.put("operationName", operation);

        incrementCounter(METRICS_COUNTER_NAME, tagsAndValues);
    }

    private void incrementCounter(String counterName, Map<String, String> tagsAndValues) {
        var counterId = counterName + tagsAndValues.toString();
        counterByNameAndTagValues.putIfAbsent(counterId, buildCounter(counterName, tagsAndValues));

        if (logger.isDebugEnabled()) {
            logger.debug("Incrementing counter for " + tagsAndValues);
        }

        counterByNameAndTagValues.get(counterId).increment();
    }

    private Counter buildCounter(String name, Map<String, String> tagsAndValues) {
        final var counterBuilder = Counter.builder(name);
        tagsAndValues.forEach(counterBuilder::tag);
        return counterBuilder.register(meterRegistry);
    }

}
