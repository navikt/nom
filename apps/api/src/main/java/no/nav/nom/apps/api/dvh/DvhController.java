package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgenhet.contract.Orgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetDvhService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@RequiredArgsConstructor
public class DvhController {
    private final OrgenhetDvhService orgenhetDvhService;

    // For analyse
    @GetMapping("/internal/dvh/events/enheter")
    public List<Orgenhet> dump() {
        return orgenhetDvhService.getAllEnheter().stream()
                .collect(toUnmodifiableList());
    }
}
