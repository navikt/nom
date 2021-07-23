package no.nav.nom.apps.api.graphql.common;

import graphql.kickstart.tools.SchemaParserDictionary;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
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

    @Bean
    public SchemaParserDictionary schemaParserDictionary() {
        return new SchemaParserDictionary()
                .add(Ressurs.class)
                .add(Organisasjonsenhet.class);
    }
}
