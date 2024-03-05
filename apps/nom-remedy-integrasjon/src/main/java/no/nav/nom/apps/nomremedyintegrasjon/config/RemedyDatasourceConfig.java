package no.nav.nom.apps.nomremedyintegrasjon.config;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "remedyEntityManagerFactory",
    transactionManagerRef = "remedyTransactionManager",
    basePackages = {
        "no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy",
        "no.nav.nom.apps.nomremedyintegrasjon.fysiskbrukersted.repository"
    }
)
public class RemedyDatasourceConfig {

    @Bean(name="remedyDataSourceProperties")
    @ConfigurationProperties("spring.datasource.remedy")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "remedyDataSource")
    public DataSource dataSource(
        @Qualifier("remedyDataSourceProperties") DataSourceProperties dataSourceProperties
    ){

        return dataSourceProperties.initializeDataSourceBuilder()
            .build();
    }

    @Bean(name = "remedyEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
        EntityManagerFactoryBuilder builder, @Qualifier("remedyDataSource") DataSource dataSource
    ){

        return builder.dataSource(dataSource)
            .packages(
                "no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy",
                "no.nav.nom.apps.nomremedyintegrasjon.fysiskbrukersted.repository"
            )
            .persistenceUnit("remedy").build();
    }

    @Bean(name = "remedyTransactionManager")
    public PlatformTransactionManager transactionManager(
        @Qualifier("remedyEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
