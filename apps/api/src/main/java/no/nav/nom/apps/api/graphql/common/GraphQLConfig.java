package no.nav.nom.apps.api.graphql.common;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GraphQLConfig {

    @Bean
    GraphQLScalarType date() {
        return ExtendedScalars.Date;
    }

    @Bean
    GraphQLScalarType dateTime() {
        return LocalDateTimeCoercing.createScalar();
    }

    @Bean
    GraphQLScalarType json() {
        return ExtendedScalars.Json;
    }
}
