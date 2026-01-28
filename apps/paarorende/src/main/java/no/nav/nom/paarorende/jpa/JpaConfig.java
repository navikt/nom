package no.nav.nom.paarorende.jpa;

import no.nav.nom.paarorende.common.SecurityUtil;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackageClasses = KontaktinfoRepository.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EntityScan(basePackageClasses = Kontaktinfo.class)
public class JpaConfig {

    private final SecurityUtil securityUtil;

    public JpaConfig(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(securityUtil.getNavIdent());
    }
}
