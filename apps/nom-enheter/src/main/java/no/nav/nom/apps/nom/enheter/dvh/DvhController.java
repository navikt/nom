package no.nav.nom.apps.nom.enheter.dvh;

import lombok.RequiredArgsConstructor;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping(value = "/dvh", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Profile("!local & !test")
public class DvhController {

    private final DvhConsumer dvhConsumer;

    @PostMapping("/load")
    public void loadFromDvh(){
        dvhConsumer.dvhScheduler();
    }
}
