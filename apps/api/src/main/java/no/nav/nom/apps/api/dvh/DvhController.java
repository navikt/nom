package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
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
    public List<DvhOrganisasjonsenhet> dump() {
        return repository.getAllEnheter().stream()
                .collect(toUnmodifiableList());
    }
}
