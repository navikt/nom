package no.nav.nom.apps.nom.unit4.input;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/ressurs")
public class RessursControllerV2 {

    private final Unit4Client unit4Client;

    @PostMapping
    public String createRessursByProxy(@RequestBody NomRessursDTO ressursDTO){
        return unit4Client.createRessursByProxy(ressursDTO).getBody();
    }

}
