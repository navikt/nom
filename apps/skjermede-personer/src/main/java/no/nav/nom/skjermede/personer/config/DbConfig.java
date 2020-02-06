package no.nav.nom.skjermede.personer.config;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = {"no.nav.nom.skjermede.personer"})
public class DbConfig {

    @Value("${postgres.jdbc.url}")
    private String jdbcUrl;

    @Value("${postgres.user.role}")
    private String role;

    @Value("${postgres.mountPath}")
    private String mountPath;

    @Bean
    @SneakyThrows
    public DataSource datasource() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);

        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, mountPath, role);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.POSTGRESQL);

        final LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(datasource());
        entityManagerFactory.setPackagesToScan("no.nav.nom.skjermede.personer");
        entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

        return entityManagerFactory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory entityManagerFactory) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

    @Bean
    public FlywayConfigurationCustomizer flywayConfig(final DataSource datasource) {
        return configuration -> configuration.initSql(String.format("SET ROLE \"%s\"", role)).dataSource(datasource);
    }
}
