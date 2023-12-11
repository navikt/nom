package no.nav.nom.apps.api.ressurs.api.v1.navident;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.OpprettRessursRequest;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.RemedyController;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.RessursDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Slf4j
@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    // TODO: Consider using RessursResolver methods to avoid duplicating code in here.
    public static final String BASE_URL = "/api/v1/navident";
    private final RemedyController remedyController;

    public NavidentController(
        RemedyController remedyController
    ) {
        this.remedyController = remedyController;
    }


    @ExceptionHandler({AccessDeniedException.class})
    @ResponseBody
    public ResponseEntity<NavidentResponse> handleAccessDeniedException() {
        return ResponseEntity.ok(
            NavidentResponse.builder()
                .status(String.valueOf(HttpStatus.FORBIDDEN.value()))
                .feilmelding("Forbidden")
                .build()
        );
    }

    @PostMapping
    @Transactional
    @Operation(
        description = "Henter en eksisterende eller oppretter en ny navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success")})
    @PreAuthorize("hasAuthority('QUERY_PERSON')")
    public ResponseEntity<NavidentResponse> opprettNavident(@RequestBody NavidentRequest request) {
        log.info(SECURE_LOGS, "opprettNavident kalt fra Remedy:\n{}", request);

        return ResponseEntity.ok(
            toNavidentResponse(remedyController.tryOpprettRessurs(convert(request)))
        );
    }

    private OpprettRessursRequest convert(NavidentRequest request) {
        return OpprettRessursRequest.builder()
            .personident(request.getPersonident())
            .fornavn(request.getFornavn())
            .mellomnavn(request.getMellomnavn())
            .etternavn(request.getEtternavn())
            .forbokstav(request.getForbokstav())
            .identType(request.getIdentType())
            .sektor(request.getSektor())
            .submitter(request.getSubmitter())
            .build();
    }

    private NavidentResponse toNavidentResponse(RessursDTO ressurs) {
        return NavidentResponse.builder()
            .status(Integer.toString(HttpStatus.OK.value()))
            .personident(ressurs.getPersonident())
            .navident(ressurs.getNavident())
            .fornavn(ressurs.getNavn().getFornavn())
            .mellomnavn(ressurs.getNavn().getMellomnavn())
            .etternavn(ressurs.getNavn().getEtternavn())
            .build();
    }
}
