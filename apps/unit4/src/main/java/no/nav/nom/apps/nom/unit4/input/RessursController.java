package no.nav.nom.apps.nom.unit4.input;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ressurs")
public class RessursController {
    private final Unit4Client unit4Client;

    @PostMapping
    @SneakyThrows
    public void createRessursByProxy(@RequestBody NomRessursDTO ressursDTO){
        unit4Client.createRessursByProxy(new Unit4RessursDto(ressursDTO));
    }
}
