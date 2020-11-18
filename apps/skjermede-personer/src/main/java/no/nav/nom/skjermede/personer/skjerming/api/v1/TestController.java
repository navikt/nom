package no.nav.nom.skjermede.personer.skjerming.api.v1;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@AllArgsConstructor
@RequestMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class TestController {


    private final NomRessurserService nomRessurserService;

    @GetMapping("testtest")
    public RessursDTO test(@RequestHeader String navident){
        log.info("Motatt forespørsel");
        return nomRessurserService.getInternRessursByNavident(navident, false);
    }
}
