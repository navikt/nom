package no.nav.nom.apps.api.graphql.common.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import no.nav.nom.apps.api.graphql.common.LocalDateTimeCoercing;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration(proxyBeanMethods = false)
public class GraphQLConfig implements RuntimeWiringConfigurer {

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.Date)
                .scalar(LocalDateTimeCoercing.createScalar());
    }
}
