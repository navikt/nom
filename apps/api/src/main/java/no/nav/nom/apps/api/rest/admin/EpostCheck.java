package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin for å sjekke om epost tjenesten går.")
@RestController
@RequestMapping("/api/admin/EpostCheck")
@PreAuthorize("hasAuthority('ADMIN')")
public class EpostCheck {

    private final EmailClient emailClient;

    public EpostCheck(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Operation(
        description = "Test epost sending",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/sendEmail")
    public String sendEmail(@RequestParam List<String> ids) {
        List<String> recipientList = ids.stream().map(id -> id + "@nav.no").toList();
        EmailMessage message = new EmailMessage("subject", "body", recipientList);
        emailClient.sendEmail(message);
        return "Email sent to: " + recipientList;
    }
}
