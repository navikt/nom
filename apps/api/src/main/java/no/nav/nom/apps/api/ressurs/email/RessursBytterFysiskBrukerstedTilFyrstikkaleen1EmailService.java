package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.EmailSendingUnforeseenException;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Component
public class RessursBytterFysiskBrukerstedTilFyrstikkaleen1EmailService {

    private static final  String RESSURS_START_SLUTT_VARSEL_SERVICETORGET = "ressurs-start-slutt-varsel-servicetorget.ftlh";
    private final Logger log = LoggerFactory.getLogger(RessursBytterFysiskBrukerstedTilFyrstikkaleen1EmailService.class);
    private final MicrosoftGraphClient microsoftGraphClient;
    private final Configuration freemarkerConfig;
    private final Unleash unleash;

    @Value("${app.email.servicetorget-email}") String servicetorgetEmail;

    public RessursBytterFysiskBrukerstedTilFyrstikkaleen1EmailService(
        MicrosoftGraphClient microsoftGraphClient,
        Configuration freemarkerConfig,
        Unleash unleash
    ) {
        this.microsoftGraphClient = microsoftGraphClient;
        this.freemarkerConfig = freemarkerConfig;
        this.unleash = unleash;
    }

    public void sendEmailTilServicetorget(RessursState utlopendeRessurs, String endretDato) {
        String subject = String.format("Ressurs bytter fysisk brukersted til Fyrstikkaleen 1 %s (%s)",
            utlopendeRessurs.visningsnavn().getFormattedVisningsnavn(), utlopendeRessurs.navident()
        );

        // only send email for ressurses with fysisk brukersted = Fyrstikkaleen 1 i.e 2700, add servicetorget to the receipient list
        try {
            var recipient = Arrays.asList(servicetorgetEmail.split("\\s*[,;]\\s*"));
            String body = getFormattedEmailBodyTilServicetorget(
                utlopendeRessurs.visningsnavn().getFormattedVisningsnavn(),
                utlopendeRessurs.navident(),
                utlopendeRessurs.primaryTelefonnummer(),
                utlopendeRessurs.startdato()!=null? utlopendeRessurs.startdato().toString():null,
                utlopendeRessurs.sluttdato()!=null? utlopendeRessurs.sluttdato().toString():null,
                "1",
                endretDato
            );
            EmailMessage message = new EmailMessage(subject, body, recipient);
            //TODO: extra logging, for testing purposes. Remove this after emailing is enabled
            log.info("Sender ressurs bytter fysisk brukersted til Fyrstikkaleen 1, epost til servicetorget body: {} recipients: {}", body, recipient);
            if (unleash.isEnabled("nom.ressurs-fysisk-brukersted-Fyrstikkaleen1-epost-servicetorget")) {
                microsoftGraphClient.sendEmail(message).subscribe();
            }
        } catch (ADResponseException e) {
            EmailSendingUnforeseenException.emailNotFound(utlopendeRessurs.navident());
        } catch (IOException | TemplateException e) {
            throw new EmailSendingUnforeseenException("UtloperRessurs: Feil ved generering av epost innhold");
        }
    }

    public String getFormattedEmailBodyTilServicetorget(String resursName,
                                                        String navIdent,
                                                        String resursTlfnr,
                                                        String resursStartdato,
                                                        String resursSlutdato,
                                                        String erRessursEndring,
                                                        String resursEndringsdato
    ) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = new HashMap<String, Object>();
        model.put("resursName", resursName);
        model.put("navIdent", navIdent);
        model.put("resursTlfnr", resursTlfnr != null ? resursTlfnr : "");
        model.put("resursStartdato", resursStartdato != null ? resursStartdato : "");
        model.put("resursSlutdato", resursSlutdato != null ? resursSlutdato : "");
        model.put("erRessursEndring", erRessursEndring);
        model.put("resursEndringsdato", resursEndringsdato != null ? resursEndringsdato : "");

        freemarkerConfig.getTemplate(RESSURS_START_SLUTT_VARSEL_SERVICETORGET).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }
}
