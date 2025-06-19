package no.nav.nom.apps.api.graphql.common.metrics;

import graphql.schema.DataFetchingEnvironment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class GraphQLFieldConsumptionCounter {

    private final static String METRICS_COUNTER_NAME = "graphql_query_field_counter";
    private final static Logger logger = LoggerFactory.getLogger(GraphQLFieldConsumptionCounter.class);

    private final MeterRegistry meterRegistry;
    private final SecurityUtil securityUtil;
    private final Map<String, Counter> counterByNameAndTagValues = new HashMap<>();

    @Autowired
    public GraphQLFieldConsumptionCounter(MeterRegistry meterRegistry, SecurityUtil securityUtil) {
        this.meterRegistry = meterRegistry;
        this.securityUtil = securityUtil;
    }

    public void count(DataFetchingEnvironment env) {
        final var consumer = securityUtil.getAzpName().orElse("unknown");
        final var operationName = env.getOperationDefinition().getName(); // operation name is the query alias, which may be null
        final var queryRoot = env.getField().getName();

        registerValues(queryRoot, queryRoot, consumer, operationName); // register query root
        env.getSelectionSet().getFields().forEach(field -> {
            final var currentPath = String.format("%s.%s", queryRoot, field.getQualifiedName().replace("/", "."));
            registerValues(field.getName(), currentPath, consumer, operationName); // register fields beneath query root
        });
    }

    private void registerValues(String field, String path, String consumer, String operationName) {
        Map<String, String> tagsAndValues = new LinkedHashMap<>();

        tagsAndValues.put("field", field);
        tagsAndValues.put("path", path);
        tagsAndValues.put("consumer", consumer);
        tagsAndValues.put("operationName", isEmpty(operationName) ? "unknown" : operationName);

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
