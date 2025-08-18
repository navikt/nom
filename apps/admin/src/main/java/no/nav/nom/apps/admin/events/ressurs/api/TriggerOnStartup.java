package no.nav.nom.apps.admin.events.ressurs.api;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TriggerOnStartup {
    private final ValidateRessursApiStateOnStartup validateRessursApiStateOnStartup;

    public TriggerOnStartup(ValidateRessursApiStateOnStartup validateRessursApiStateOnStartup) {
        this.validateRessursApiStateOnStartup = validateRessursApiStateOnStartup;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        validateRessursApiStateOnStartup.validate();
    }
}
