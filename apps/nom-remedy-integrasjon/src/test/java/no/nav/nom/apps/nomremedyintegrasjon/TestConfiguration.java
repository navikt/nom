package no.nav.nom.apps.nomremedyintegrasjon;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
public class TestConfiguration {

    @Primary
    @Bean(name = "nomRemedyIntegrasjonDataSource")
    @Profile("test")
    public DataSource dataSource(
        @Qualifier("nomRemedyIntegrasjonDataSourceProperties") DataSourceProperties dataSourceProperties
    ) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

}
