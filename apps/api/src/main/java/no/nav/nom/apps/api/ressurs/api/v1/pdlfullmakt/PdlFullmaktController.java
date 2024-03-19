package no.nav.nom.apps.api.ressurs.api.v1.pdlfullmakt;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@Tag(name = "Pdl-fullmakt")
@RestController
@RequestMapping(PdlFullmaktController.BASE_URL)
public class PdlFullmaktController {

    public static final String BASE_URL = "/api/v1/pdlfullmakt";

    private final RessursStateRepository ressursStateRepository;

    public PdlFullmaktController(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    @PostMapping("/getAnsatt")
    @Operation(
        description = "Hent ansatt",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<AnsattPersonResponse> getAnsatt(@RequestBody AnsattPersonRequest req, Authentication auth) {
        if (!callerMatchesAllowedApps(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("mottatt kall mot /getAnsattStatus");
        var fnr = req.personIdent();
        if (fnr == null) {
            return ResponseEntity.ok(new AnsattPersonResponse(null));
        }
        var ansattPersonResponse = ressursStateRepository.findByPersonident(fnr)
            .map(it -> new AnsattPersonResponse(new AnsattPersonResponse.AnsattPerson(it.navident(),
                it.startdato().map(LocalDate::toString).orElse(null),
                it.sluttdato().map(LocalDate::toString).orElse(null),
                it.sektor()
            )))
            .orElse(new AnsattPersonResponse(null));
        return ResponseEntity.ok(ansattPersonResponse);
    }

    private boolean callerMatchesAllowedApps(Authentication auth) {
        var matchReprFullmakt = callerMatches(auth, "repr","repr-fullmakt");

        var matchPdlFullmakt = callerMatches(auth, "pdl","pdl-fullmakt");
        if(matchPdlFullmakt){
            // TODO pdl-fullmakt skal bytte navn til repr fullmakt, så når denne loggmeldingen
            // ikke lengre forekommer, kan koden endres til å kun sjekke for repr-fullmakt
            log.info("konsument pdl-fullmakt er fortsatt aktiv");
        }

        return matchReprFullmakt || matchPdlFullmakt;
    }

    private boolean callerMatches(Authentication auth, String namespace, String appname) {
        var creds = auth.getCredentials();
        if(!(creds instanceof Jwt)){
            return false;
        }
        var claims = ((Jwt) creds).getClaims();
        var azpName = claims.get("azp_name").toString();

        return matchAzpString(azpName,namespace,appname);
    }


    private boolean matchAzpString(String input, String namespace, String appname){
        var azpStringElements = input.split(":");
        if(azpStringElements.length != 3){
            return false;
        }
        var isMatch = azpStringElements[0].matches("((prod)|(dev))-((fss)|(gcp))");
        isMatch = isMatch && azpStringElements[1].equals(namespace);
        isMatch = isMatch && azpStringElements[2].equals(appname);
        return isMatch;
    }
}
