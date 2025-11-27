package no.nav.nom.apps.gql;

import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

public class GraphQLConfig implements RuntimeWiringConfigurer {

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.Date)
                .scalar(LocalDateTimeCoercing.createScalar());
    }

    @Bean
    GraphQlSourceBuilderCustomizer inspectionCustomizer() {
        return source -> source.inspectSchemaMappings(report -> {
            var noUnmappedRegistrations = report.unmappedRegistrations().isEmpty();
            var noUnmappedArgs = report.unmappedArguments().isEmpty();
            var noUnmappedFields = report.unmappedFields().isEmpty();
            if (!(noUnmappedArgs && noUnmappedFields && noUnmappedRegistrations)) {
                throw new RuntimeException(report.toString());
            }
        });
    }
}
