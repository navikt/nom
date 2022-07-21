package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.idgenerator.NavidRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackageClasses = {NavidRepository.class})
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditor() {
        return () -> {
            // Må oppdateres med info fra tokenet når spring-security er på plass
            return Optional.of("SYSTEMBRUKER");
        };
    }
}
