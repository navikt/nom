package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@RequiredArgsConstructor
public class DvhController {
    private final DvhRepository repository;

    // For analyse
    @GetMapping("/internal/dvh/events/enheter")
    public List<DvhEvent> dump() {
        return repository.findAll().stream()
                .filter(e -> Enhet.TYPE.equals(e.getType()))
                .collect(toUnmodifiableList());
    }
}
