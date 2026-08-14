package no.nav.nom.apps.api.tilgangsenhet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tilgangsenhet")
@RestController
@RequestMapping(value = {TilgangsenhetController.BASE_URL})
public class TilgangsenhetController {

    private final TilgangsenhetRepository tilgangsenhetRepository;

    public static final String BASE_URL = "/app/tilgangsenhet";

    public TilgangsenhetController(TilgangsenhetRepository tilgangsenhetRepository) {
        this.tilgangsenhetRepository = tilgangsenhetRepository;
    }

    @GetMapping
    @Operation(
        description = "Henter ut alle tilgangsenhetIder fra NORG",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAnyAuthority('ADMIN', 'NOM_UI_APP')")
    public List<Tilgangsenhet> getAll() {
        return tilgangsenhetRepository.getAll();
    }
}
