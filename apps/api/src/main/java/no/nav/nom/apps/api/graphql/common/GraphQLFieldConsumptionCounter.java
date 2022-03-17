package no.nav.nom.apps.api.graphql.common;

import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class GraphQLFieldConsumptionCounter {
    private final SecurityUtil securityUtil;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterByNameAndTagValues;

    public GraphQLFieldConsumptionCounter(SecurityUtil securityUtil, MeterRegistry meterRegistry){
        this.securityUtil = securityUtil;
        this.meterRegistry = meterRegistry;
        this.counterByNameAndTagValues = new HashMap<>();
    }


    public void count(DataFetchingEnvironment env){
        val operationName = Optional.ofNullable(env.getOperationDefinition().getName()).orElse("unknown");
        val consumer = securityUtil.getAzpName(env).orElse(securityUtil.getAzp(env).orElse("unknown"));
        count(env.getField().getName(), env.getField().getSelectionSet(), operationName, consumer);
    }

    public void count(String path, SelectionSet selectionSet, String operationName, String consumer){
        selectionSet.getSelections().forEach(s -> {
            Field field = (Field) s;
            if(field.getSelectionSet() != null) {
                count(path+"."+field.getName(), field.getSelectionSet(), operationName, consumer);
            }
            else {
                val tagsAndValues = new LinkedHashMap<String, String>();
                tagsAndValues.put("operation", operationName);
                tagsAndValues.put("path", path+"."+field.getName());
                tagsAndValues.put("consumer", consumer);
                tagsAndValues.put("field", field.getName());

                incrementCounter("graphql_query_field", tagsAndValues);
            }
        });
    }

    private void incrementCounter(String counterName, LinkedHashMap<String, String> tagsAndValues){
        var counterId = counterName+tagsAndValues.toString();
        counterByNameAndTagValues.putIfAbsent(counterId, buildCounter(counterName, tagsAndValues));
        counterByNameAndTagValues.get(counterId).increment();
    }

    private Counter buildCounter(String name, LinkedHashMap<String, String> tagsAndValues){
        val counterBuilder = Counter.builder(name);
        tagsAndValues.forEach(counterBuilder::tag);
        return counterBuilder.register(meterRegistry);
    }
}
