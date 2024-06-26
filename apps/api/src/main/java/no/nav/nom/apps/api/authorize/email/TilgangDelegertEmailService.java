package no.nav.nom.apps.api.authorize.email;

import freemarker.template.Configuration;
import no.nav.nom.apps.api.authorize.redigereressurs.api.LederTilgangForOrgenhetRequest;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TilgangDelegertEmailService {
    private static final String DELEGERT_TILGANG_TEMPLATE = "delegert-tilgang.ftlh";

    private final Logger log = LoggerFactory.getLogger(TilgangDelegertEmailService.class);
    private final EmailClient emailClient;
    private final AdClient adClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final Configuration freemarkerConfig;


    public TilgangDelegertEmailService(
        EmailClient emailClient,
        AdClient adClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        Configuration freemarkerConfig
    ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.freemarkerConfig = freemarkerConfig;
    }

    public void notifyDelegertTilgangByEmail(String lederNavident, LederTilgangForOrgenhetRequest request) {
        try {
            List<String> navIdenter = List.of(lederNavident, request.delegertTilNavident());
            var adRessurser = adClient.fetchInAdRessursInBatches(navIdenter);
            var orgEnhet = orgEnhetStateRepository.findById(request.orgenhetId()).orElseThrow(() -> new OrgEnhetNotFoundException(request.orgenhetId()));
            composeAndSendEmail(adRessurser.get(lederNavident), adRessurser.get(request.delegertTilNavident()), request.gyldigTom(), orgEnhet);
        } catch (ADResponseException e) {
            throw new ADResponseException("Fant ikke leder eller delegert ressurs i AD", e.getErrorCode());
        }
    }

    private void composeAndSendEmail(AdRessurs leder, AdRessurs delegertRessurs, LocalDate gyldigTom, OrgEnhetState orgEnhet) {
        String subject = String.format("Tilgang delegert til %s (%s)", delegertRessurs.getDisplayName(), delegertRessurs.getNavIdent());
        var recipientList = List.of(leder.getEmail(), delegertRessurs.getEmail());
        String body = getFormattedEmailBody(
            leder.getDisplayName(),
            leder.getNavIdent(),
            delegertRessurs.getDisplayName(),
            delegertRessurs.getNavIdent(),
            gyldigTom,
            orgEnhet.navn());

        log.info("Sender epost emne {} til {}", subject, recipientList);

        var emailMessage = new EmailMessage(subject, body, recipientList);
        emailClient.sendEmail(emailMessage).subscribe();
    }

    private String getFormattedEmailBody(String lederDisplayname, String lederNavIdent, String delegertRessursDisplayname, String delegertRessursNavident, LocalDate gyldigTom, String orgenhetNavn) {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "lederDisplayname", lederDisplayname,
            "lederNavIdent", lederNavIdent,
            "delegertRessursDisplayname", delegertRessursDisplayname,
            "delegertRessursNavident", delegertRessursNavident,
            "tomDato", gyldigTom,
            "orgenhetNavn", orgenhetNavn);
        try {
            freemarkerConfig.getTemplate(DELEGERT_TILGANG_TEMPLATE).process(model, stringWriter);
            return stringWriter.getBuffer().toString();
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke formatere epost", e);
        }
    }
}
