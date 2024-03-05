package no.nav.nom.apps.nomremedyintegrasjon.config;

import com.zaxxer.hikari.HikariConfig;

import jakarta.persistence.EntityManagerFactory;

import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "nomRemedyIntegrasjonEntityManagerFactory",
    transactionManagerRef = "nomRemedyIntegrasjonTransactionManager",
    basePackages = {
        "no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent"
    }
)
public class NomRemedyIntegrasjonDataSourceConfig {

    @Primary
    @Bean(name = "nomRemedyIntegrasjonDataSourceProperties")
    @ConfigurationProperties("spring.datasource.nom-remedy-integrasjon")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "nomRemedyIntegrasjonDataSource")
    @SneakyThrows
    @Profile({"prod", "dev"})
    public DataSource dataSource(
        @Qualifier("nomRemedyIntegrasjonDataSourceProperties") DataSourceProperties dataSourceProperties,
        @Value("${vault.postgresMountPath}") String vaultPostgresMountPath
    ){

        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dataSourceProperties.getUrl());
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setMinimumIdle(1);

        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            vaultPostgresMountPath,
            dataSourceProperties.getUsername()
        );
    }

    @Primary
    @Bean(name = "nomRemedyIntegrasjonEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory
        (EntityManagerFactoryBuilder builder, @Qualifier("nomRemedyIntegrasjonDataSource") DataSource dataSource){

        return builder.dataSource(dataSource)
            .packages("no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent")
            .persistenceUnit("nom-remedy-integrasjon").build();
    }

    @Primary
    @Bean(name = "nomRemedyIntegrasjonTransactionManager")
    public PlatformTransactionManager transactionManager(
        @Qualifier("nomRemedyIntegrasjonEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    @Profile({"prod", "dev"})
    public FlywayConfigurationCustomizer flywayConfig(
        @Qualifier("nomRemedyIntegrasjonDataSource") DataSource datasource,
        @Qualifier("nomRemedyIntegrasjonDataSourceProperties") DataSourceProperties dataSourceProperties
    ) {
        return configuration -> configuration.initSql(String.format("SET ROLE \"%s\"", dataSourceProperties.getUsername()))
            .dataSource(datasource);
    }
}
