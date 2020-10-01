package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/internal")
@Profile("!local")
public class InternalController {

    private final DvhConsumer dvhConsumer;

    @GetMapping("/updatefromdvh")
    public void updateFromDvh(){
        dvhConsumer.dvhScheduler();
    }
}
