package no.nav.nom.apps.api.graphql.common.metrics;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class GraphQLFieldConsumptionCounter {
//    private final SecurityUtil securityUtil;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterByNameAndTagValues = new HashMap<>();

    @Autowired
    public GraphQLFieldConsumptionCounter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void count(DataFetchingEnvironment env) {
        final var consumer = getAzpName().orElse("unknown");
        final var operationName = env.getOperationDefinition().getName();
        count(null, env.getField(), consumer, operationName != null && !operationName.isEmpty() ? operationName : "unknown");
    }

    public void count(String path, Field field, String consumer, String operationName) {
        var currentPath = path != null && !path.isEmpty() ? path + "." + field.getName() : field.getName();
        final var tagsAndValues = new LinkedHashMap<String, String>();
        if (field.getSelectionSet() != null) {
            field.getSelectionSet().getSelections().forEach(s -> {
                if (s instanceof Field) {
                    count(currentPath, (Field) s, consumer, operationName);
                } else {
                    // TODO, also count fields within Fragments or inline Fragments
                }
            });
            tagsAndValues.put("type", "complex-object");
        } else {
            tagsAndValues.put("type", "simple");
        }
        tagsAndValues.put("field", field.getName());
        tagsAndValues.put("path", currentPath);
        tagsAndValues.put("consumer", consumer);
        tagsAndValues.put("operationName", operationName);
        incrementCounter("graphql_query_field_counter", tagsAndValues);
    }

    private void incrementCounter(String counterName, LinkedHashMap<String, String> tagsAndValues) {
        var counterId = counterName + tagsAndValues.toString();
        counterByNameAndTagValues.putIfAbsent(counterId, buildCounter(counterName, tagsAndValues));
        counterByNameAndTagValues.get(counterId).increment();
    }

    private Counter buildCounter(String name, LinkedHashMap<String, String> tagsAndValues) {
        final var counterBuilder = Counter.builder(name);
        tagsAndValues.forEach(counterBuilder::tag);
        return counterBuilder.register(meterRegistry);
    }

    private Optional<String> getAzpName() {
        // TODO: add security enabled switch?
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var azp_name = auth instanceof JwtAuthenticationToken ? ((JwtAuthenticationToken) auth).getTokenAttributes().get("azp_name") : null;
        return Optional.ofNullable(azp_name != null ? azp_name.toString() : null);
    }
}
