package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RessursOpprettetEmailService {
    private static final String OPPRETTELSE_VARSEL_TEMPLATE = "opprettelse-varsel.ftlh";
    public static final String NAVIDENT = "NAVident";

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);
    private final MicrosoftGraphClient microsoftGraphClient;
    private final AdClient adClient;
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetLederService orgEnhetLederService;
    private final Configuration freemarkerConfig;

    public RessursOpprettetEmailService(
        MicrosoftGraphClient microsoftGraphClient,
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        Configuration freemarkerConfig,
        OrgEnhetLederService orgEnhetLederService
    ) {
        this.microsoftGraphClient = microsoftGraphClient;
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetLederService = orgEnhetLederService;
        this.freemarkerConfig = freemarkerConfig;
    }

    public void notifyByEmail(RessursState opprettetRessurs, OrgEnhetState orgEnhet) {
        log.info("Sender epost om opprettelse av ressurs {} til leder av orgenhet {}", opprettetRessurs.navident(), orgEnhet.id());

        var leder = orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhet.id());

        if(leder.isEmpty()) {
            throw new RessursOpprettetEmailException("Fant ikke leder for orgenhet " + orgEnhet.id());
        }

        // compose and send email to all recipients
        Set <String> recipientNavidents = Set.of(leder.get().navident());
        recipientNavidents = appendAuthenticatedUserToRecipientNavIdents(Set.copyOf(recipientNavidents));
        composeAndSendEmail(opprettetRessurs, recipientNavidents, orgEnhet.navn());
    }

    private void composeAndSendEmail(RessursState opprettetRessurs, Set<String> recipientNavidents, String orgenhetName) {
        try {
            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

            String subject = String.format("Ressurs opprettet %s (%s)",
                opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident()
            );
            String body = getFormattedEmailBody(
                opprettetRessurs.visningsnavn().getFormattedVisningsnavn(),
                opprettetRessurs.navident(),
                orgenhetName
            );

            //TODO: extra logging, for testing purposes. Remove this after emailing is enabled
            log.info("Sender epost subject: {} recipients: {}", subject, recipientList);

            EmailMessage message = new EmailMessage(subject, body, recipientList);
            microsoftGraphClient.sendEmail(message).subscribe();

        } catch (ADResponseException e) {
            throw new RessursOpprettetEmailException("Fant ikke leder epost adresse for leder");
        } catch (IOException | TemplateException e) {
            throw new RessursOpprettetEmailException("Feil ved generering av epost innhold");
        }
    }

    public String getFormattedEmailBody(String resursName, String navIdent, String orgenhetName) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "resursName", resursName,
            "navIdent", navIdent,
            "orgenhetName", orgenhetName);
        freemarkerConfig.getTemplate(OPPRETTELSE_VARSEL_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    private String tilOrgEnhetsLederNavIdents(Ressurs leder) {
        return leder.getNavIdent();
    }

    public Set<String> appendAuthenticatedUserToRecipientNavIdents(Set<String> recipientNavIdents) {
        List<String> updatedRecipientsNavIdents = new ArrayList<>(recipientNavIdents);
        String navIdentOfAuthenticatedUser = getAuthenticatedNavIdent();
        if(!navIdentOfAuthenticatedUser.isEmpty() && !recipientNavIdents.contains(navIdentOfAuthenticatedUser)){
            updatedRecipientsNavIdents.add(navIdentOfAuthenticatedUser);
        }
        return Set.copyOf(updatedRecipientsNavIdents);
    }

    private static String getAuthenticatedNavIdent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getNavIdent(authentication).orElse("");
    }

    private static Optional<String> getNavIdent(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Optional.ofNullable(jwt.getClaimAsString(NAVIDENT));
    }

    public List<String> appendAdditionalRecipientsEmails(List<String> recipients, List<String> additionalRecipients) {
        List<String> updatedRecipients = new ArrayList<>(recipients);
        for (String additionalRecipient : additionalRecipients) {
            // ignore cases in the email address
            if (!updatedRecipients.contains(additionalRecipient) && !updatedRecipients.contains(additionalRecipient.toLowerCase())) {
                updatedRecipients.add(additionalRecipient);
            }
        }
        return Collections.unmodifiableList(updatedRecipients);
    }
}


