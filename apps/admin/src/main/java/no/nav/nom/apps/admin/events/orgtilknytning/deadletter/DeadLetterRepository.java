package no.nav.nom.apps.admin.events.orgtilknytning.deadletter;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.deadletter.DeadLetterDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class DeadLetterRepository {
    private final List<DeadLetterDTO> deadLetters = new ArrayList<>();

    public void addDeadLetter(DeadLetterDTO orgtilknytningDeadLetterDTO) {
        deadLetters.add(orgtilknytningDeadLetterDTO);
    }

    public int count() {
        return deadLetters.size();
    }
}
