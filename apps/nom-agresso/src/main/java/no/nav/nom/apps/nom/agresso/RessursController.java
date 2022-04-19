package no.nav.nom.apps.nom.agresso;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ressurs")
public class RessursController {

    @PostMapping
    public RessursDTO createRessurs(@RequestBody RessursDTO ressursDTO){
        return ressursDTO;
    }
}
