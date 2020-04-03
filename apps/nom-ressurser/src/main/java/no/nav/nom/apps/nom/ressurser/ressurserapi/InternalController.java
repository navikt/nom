package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.ressurser.dvh.DvhConsumer;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/internal")
@Profile("!local")
public class InternalController {

    private final DvhConsumer dvhConsumer;

    @Unprotected
    @GetMapping("/updatefromdvh")
    public void updateFromDvh(){
        dvhConsumer.dvhScheduler();
    }
}
