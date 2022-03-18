package no.nav.nom.apps.api.graphql.common;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
        val consumer = securityUtil.getAzpName(env).orElse(securityUtil.getAzp(env).orElse("unknown"));
        val operationName = env.getOperationDefinition().getName();
        count(null, env.getField(), consumer, operationName != null && !operationName.isEmpty() ? operationName : "unknown");
    }

    public void count(String path, Field field, String consumer, String operationName){
        val tagsAndValues = new LinkedHashMap<String, String>();
        if(field.getSelectionSet() != null) {
            field.getSelectionSet().getSelections().forEach(s -> count(path, (Field) s, consumer, operationName));
            tagsAndValues.put("type", "complex-object");
        }
        else {
            tagsAndValues.put("type", "simple");
        }
        tagsAndValues.put("field", field.getName());
        tagsAndValues.put("path", path != null && !path.isEmpty() ? path+"."+field.getName() : field.getName());
        tagsAndValues.put("consumer", consumer);
        tagsAndValues.put("operationName", operationName);
        incrementCounter("graphql_query_field_counter", tagsAndValues);





        /*selectionSet.getSelections().forEach(s -> {
            Field field = (Field) s;
            val tagsAndValues = new LinkedHashMap<String, String>();
            if(field.getSelectionSet() != null) {
                count(path+"."+field.getName(), field.getSelectionSet(), consumer);
                tagsAndValues.put("type", "complex-object");
            }
            else {
                tagsAndValues.put("type", "simple");
            }
            tagsAndValues.put("field", field.getName());
            tagsAndValues.put("path", path+"."+field.getName());
            tagsAndValues.put("consumer", consumer);
            incrementCounter("graphql_query_field", tagsAndValues);

        });*/
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
