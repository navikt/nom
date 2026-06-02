package no.nav.nom.apps.api.tilgangsenhet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = TilgangsenhetController.BASE_URL)
public class TilgangsenhetController {

    private final TilgangsenhetRepository tilgangsenhetRepository;

    public static final String BASE_URL = "/tilgangsenhet";

    public TilgangsenhetController(TilgangsenhetRepository tilgangsenhetRepository) {
        this.tilgangsenhetRepository = tilgangsenhetRepository;
    }

    @GetMapping
    public List<Tilgangsenhet> getAll() {
        return tilgangsenhetRepository.getAll();
    }
}
