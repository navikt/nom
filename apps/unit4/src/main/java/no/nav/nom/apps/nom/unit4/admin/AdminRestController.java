package no.nav.nom.apps.nom.unit4.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.nom.unit4.common.SwaggerConfig;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Exception;
import no.nav.nom.apps.nom.unit4.unit4.Unit4NotFoundException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminRestController {
    private final Logger log = LoggerFactory.getLogger(AdminRestController.class);

    private final Unit4Client unit4Client;

    public AdminRestController(Unit4Client unit4Client) {
        this.unit4Client = unit4Client;
    }

    @Operation(
        description = "Finn Unit4 ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping(value = "/ressurs/{ressursId}")
    public String finnRessurs(@PathVariable String ressursId) {
        try {
            return unit4Client.findRessurs(ressursId).toPrettyString();
        } catch (Unit4NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Unit4RequestException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Unit4Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
