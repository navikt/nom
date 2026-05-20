package no.nav.nom.apps.api.tilgangsenhet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = TilgangsenhetController.BASE_URL)
public class TilgangsenhetController {

    private final TilgangsenhetKandidat tilgangsenhetKandidat;

    public static final String BASE_URL = "/tilgangsenhet";

    public TilgangsenhetController(TilgangsenhetKandidat tilgangsenhetKandidat) {
        this.tilgangsenhetKandidat = tilgangsenhetKandidat;
    }

    @GetMapping
    public List<Tilgangsenhet> getAll() {
        return tilgangsenhetKandidat.getAll();
    }
}
