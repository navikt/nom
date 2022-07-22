package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.idgenerator.NavidRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackageClasses = {NavidRepository.class})
@EnableJpaAuditing
public class JpaConfig {
    @Bean
    public AuditorAware<String> auditor() {
        return () -> {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
                return Optional.of(auth.getToken().getClaim("sub").toString());
            } else {
                return Optional.of("SYSTEMBRUKER");
            }
        };
    }
}
