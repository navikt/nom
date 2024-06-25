package no.nav.nom.apps.api.rest.admin;

import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Tag(name = "Admin for å sjekke om epost tjenesten går.")
@RestController
@RequestMapping("/api/admin/EpostCheck")
@PreAuthorize("hasAuthority('ADMIN')")
public class EpostCheck {

    private final EmailClient emailClient;
    private final RessursOpprettetEmailService opprettetRessurs;

    public EpostCheck(EmailClient emailClient, RessursOpprettetEmailService opprettetRessurs) {
        this.emailClient = emailClient;
        this.opprettetRessurs = opprettetRessurs;
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
    public String sendEmail(@RequestParam List<String> ids) throws TemplateException, IOException {
        List<String> recipientList = ids.stream().map(id -> id + "@nav.no").toList();

        String testBody = opprettetRessurs.getFormattetEmailBody(
            "Test ressurs",
            "Test navident",
            "Test orgenhetName"
        );

        EmailMessage message = new EmailMessage("subject", testBody, recipientList);
        emailClient.sendEmail(message).subscribe();
        return "Email sent to: " + recipientList;
    }
}
