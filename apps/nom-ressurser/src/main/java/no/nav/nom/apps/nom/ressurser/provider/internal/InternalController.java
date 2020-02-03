package no.nav.nom.apps.nom.ressurser.provider.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InternalController.BASE_URL)
public class InternalController {

    public static final String BASE_URL = "/internal";

    private static final String IS_ALIVE = "isAlive";
    private static final String IS_READY = "isReady";

    @GetMapping(IS_ALIVE)
    public void isAlive() {}

    @GetMapping(IS_READY)
    public void isReady() {}
}

