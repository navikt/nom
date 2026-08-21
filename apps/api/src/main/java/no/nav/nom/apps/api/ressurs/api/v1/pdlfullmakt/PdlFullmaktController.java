package no.nav.nom.apps.api.ressurs.api.v1.pdlfullmakt;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Pdl-fullmakt")
@RestController
@RequestMapping(PdlFullmaktController.BASE_URL)
@PreAuthorize("hasAuthority(\"FULLMAKT\")")
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
        log.info("mottatt kall mot /getAnsattStatus");
        var fnr = req.personIdent();
        if (fnr == null) {
            return ResponseEntity.ok(new AnsattPersonResponse(null));
        }
        var ansattPersonResponse = ressursStateRepository.findByPersonident(fnr)
            .map(it -> new AnsattPersonResponse(new AnsattPersonResponse.AnsattPerson(
                it.navident(),
                it.startdato() != null ? it.startdato().toString() : null,
                it.sluttdato() != null ? it.sluttdato().toString() : null,
                it.sektor()
            )))
            .orElse(new AnsattPersonResponse(null));
        return ResponseEntity.ok(ansattPersonResponse);
    }
}
