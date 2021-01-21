package no.nav.nom.apps.api.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile("!test")
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
