package no.nav.nom.apps.nomremedyintegrasjon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.r2dbc.EventReadConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableR2dbcRepositories(
    basePackages = {
        "no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.r2dbc"
    }
)
public class NomRemedyReactiveDatasourceConfig {

    @Bean(name = "nomRemedyR2dbcProperties")
    @Primary
    @ConfigurationProperties("spring.r2dbc")
    public R2dbcProperties r2dbcProperties() {
        return new R2dbcProperties();
    }

    @Bean
    public ConnectionFactory connectionFactory(@Qualifier("nomRemedyR2dbcProperties") R2dbcProperties r2dbcProperties) {
        return ConnectionFactoryBuilder.withUrl(r2dbcProperties.getUrl()).username(r2dbcProperties.getUsername()).build();
    }

    @Bean
    public R2dbcTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
        List<Object> converters = new ArrayList<>();
        converters.add(new EventReadConverter(objectMapper));
        return new R2dbcCustomConversions(CustomConversions.StoreConversions.NONE, converters);
    }
}
