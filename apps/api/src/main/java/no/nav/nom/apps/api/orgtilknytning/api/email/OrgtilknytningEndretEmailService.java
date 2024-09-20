package no.nav.nom.apps.api.orgtilknytning.api.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrgtilknytningEndretEmailService {
    private static final String OT_ENDRINGER_VARSEL_TEMPLATE = "ot-endringer-varsel.ftlh";
    private static final String UNLEASH_USE_SEND_OT_ENDRINGER_EMAIL = "nom.orgtilknytning.send_ot_endringer_email";
    private final Map<String, List<OTEndretData>> orgtilknytningEndringerQueueByNavident = new ConcurrentHashMap<>();
    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);
    private final EmailClient emailClient;
    private final Unleash unleash;
    private final Configuration freemarkerConfig;
    private final AdClient adClient;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public OrgtilknytningEndretEmailService(
        EmailClient emailClient,
        Unleash unleash,
        Configuration freemarkerConfig,
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.emailClient = emailClient;
        this.unleash = unleash;
        this.freemarkerConfig = freemarkerConfig;
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public void addToOrgtilknytningEndringerQueue(String navident, OTEndretData otEndretData) {
        if(!orgtilknytningEndringerQueueByNavident.containsKey(navident)){
            orgtilknytningEndringerQueueByNavident.put(navident, new ArrayList<>(Collections.singletonList(otEndretData)));
        } else {
            orgtilknytningEndringerQueueByNavident.get(navident).add(otEndretData);
        }
    }

    public void removeFromOrgtilknytningEndringerQueue(String navident) {
        orgtilknytningEndringerQueueByNavident.remove(navident);
    }

    public void clear() {
        orgtilknytningEndringerQueueByNavident.clear();
    }

    @Scheduled(cron = "0 0 18 * * ?") // every day at 18:00
    public void sendEmail() {
        orgtilknytningEndringerQueueByNavident.forEach((navident, endringer) -> {
            var endretRessurs = ressursStateRepository.findByNavident(navident);
            if(endretRessurs.isEmpty()) {
                log.warn("Fant ikke ressurs med navident {} og epost varsel vil ikke bli sendt.", navident);
                // clear from the queue
                removeFromOrgtilknytningEndringerQueue(navident);
            } else {
                sendEmail(endretRessurs.get(), Set.of(navident), endringer);
                removeFromOrgtilknytningEndringerQueue(navident);
            }
        });
    }

    private void sendEmail(RessursState endretRessurs, Set<String> recipientNavidents, List<OTEndretData> otEndringerData) {
        try {
            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

            String subject = String.format("Endringer i Orgtilknytninger %s (%s)",
                endretRessurs.visningsnavn().getFormattedVisningsnavn(), endretRessurs.navident()
            );

            String otEndringer = composeOTChangesMessage(otEndringerData);

            String body = getFormattetEmailBody(
                endretRessurs.visningsnavn().getFormattedVisningsnavn(),
                endretRessurs.navident(),
                otEndringer
            );

            if(body.isBlank()) {
                log.warn("Epost body er tom, OR endringer epost varsel vil ikke bli sendt");
                return;
            }

            log.info("Sender OT endringer epost subject: {}, recipients: {}, message: {}", subject, recipientList, body);
            if (unleash.isEnabled(UNLEASH_USE_SEND_OT_ENDRINGER_EMAIL)) {
                EmailMessage message = new EmailMessage(subject, body, recipientList);
                emailClient.sendEmail(message).subscribe();
            }
        } catch (ADResponseException e) {
            throw new RessursOpprettetEmailException("Fant ikke leder epost adresse for leder");
        } catch (IOException | TemplateException e) {
            throw new RessursOpprettetEmailException("Feil ved generering av epost innhold");
        }
    }

    public String getFormattetEmailBody(String resursName, String navIdent, String otEndringer) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "resursName", resursName,
            "navIdent", navIdent,
            "otEndringer", otEndringer);
        freemarkerConfig.getTemplate(OT_ENDRINGER_VARSEL_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    public String composeOTChangesMessage(List<OTEndretData> otEndringerData) {
        StringBuilder otEndringer = new StringBuilder();
        for (OTEndretData event : otEndringerData) {
            var orgtilknytningState =  orgtilknytningStateRepository.findByFid(event.fid());
            if(orgtilknytningState.isPresent()){
                var ot = orgtilknytningState.get();
                var utfortAv = event.changedBy();
                String endring = ot.orgEnhetId() + "- fra og med " + ot.gyldigFom() +", til og med "+ ot.gyldigTom() + ". Endring utført av " + utfortAv + "," + event.timestamp() + ".";
                otEndringer.append(endring);
            }

            if(!otEndringer.isEmpty()) {
                otEndringer.append("\n");
            }
        }
        return otEndringer.toString();
    }

}
