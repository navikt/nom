package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v1.OrgenhetDvhInterface;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@RequiredArgsConstructor
public class DvhController {
    private final OrgenhetDvhInterface orgenhetDvhInterface;

    // For analyse
    @GetMapping("/internal/dvh/events/enheter")
    public List<IOrgenhet> dump() {
        return orgenhetDvhInterface.getAllEnheter().stream()
                .collect(toUnmodifiableList());
    }
}
