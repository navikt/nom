package no.nav.nom.apps.api.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/email")
public class EmailTempController {

    private final EmailClient emailClient;

    public EmailTempController(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public static final Logger logger = LogManager.getLogger(EmailTempController.class);

    @GetMapping("/ping")
    public void pingEmail() {
        logger.info("Pinging email");

        EmailMessage emailMessage = new EmailMessage("Test ping Email NOM",
            "This is a ping message from NOM", List.of("eziz.annagurban@nav.no"));

        emailClient.sendEmail(emailMessage);


    }

}
