package no.nav.nom.apps.nom.agresso.input;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.agresso.agresso.AgressoClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ressurs")
public class RessursController {

    private final AgressoClient agressoClient;

    @PostMapping
    public String createRessurs(@RequestBody NomRessursDTO ressursDTO){
        return agressoClient.createRessurs(ressursDTO);
    }
}
