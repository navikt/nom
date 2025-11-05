package no.nav.nom.apps.api.graphql.common.config;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.language.Document;
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QueryLoggingInstrumentation extends SimplePerformantInstrumentation {

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters, InstrumentationState state) {
        ExecutionInput executionInput = parameters.getExecutionInput();
        String query = executionInput.getQuery();

        try {
            // Parse and remove arguments from the query
            Parser parser = new Parser();
            Document document = parser.parseDocument(query);
            String queryWithoutArgs = removeArguments(document);
            log.info("GraphQL Query: {}", queryWithoutArgs);
        } catch (Exception ignore) {}

        return super.beginExecution(parameters, state);
    }

    private String removeArguments(Document document) {
        // Use AstPrinter to get a clean string representation
        // Arguments will still appear, so use regex to remove them
        String queryString = graphql.language.AstPrinter.printAst(document);
        return queryString.replaceAll("\"[^)]*\"", "\"?\"");
    }
}