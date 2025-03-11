package no.nav.nom.apps.admin.events.ressurs.api;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Component
public class RessursHendelseApiRepository {
    private final List<RessursHendelseKafkaDTO> ressursHendelser = new ArrayList<>(); 
    
    public void add(List<RessursHendelseKafkaDTO> newRessursHendelser) {
        ressursHendelser.addAll(newRessursHendelser);
    }

    public int count() {
        return ressursHendelser.size();
    }
}
