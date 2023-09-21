package no.nav.nom.apps.api.common;


import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LocalConfiguration {

    public final boolean shouldSaveEventsToDb;

    public LocalConfiguration(Environment env) {
        this.shouldSaveEventsToDb = !env.matchesProfiles("local & skip-event-backup");
    }
}