package no.nav.nom.apps.nom.ressurser.provider.rs;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.ressurser.domain.Ressurs;
import no.nav.nom.apps.nom.ressurser.service.RessursService;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/ressurs", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RessursController {

    private final RessursService ressursService;

    @PostMapping("/opprett")
    public void opprettRessurs(@RequestBody Ressurs ressurs){
        ressursService.opprettRessurs(ressurs);
    }
}
