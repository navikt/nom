package no.nav.nom.apps.api.authorize.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class TilgangDelegertUtloeperEmailService {

    private static final String DELEGERT_TILGANG_UTLOEPER_TEMPLATE = "delegert-tilgang-utloeper.ftlh";


    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final LeaderElection leaderElection;
    private final DelegertLederStateRepository delegertLederStateRepository;
    private final AdClient adClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final Configuration freemarkerConfig;
    private final MicrosoftGraphClient microsoftGraphClient;

    public TilgangDelegertUtloeperEmailService(
        DvhStartupLoadMonitor dvhStartupLoadMonitor,
        LeaderElection leaderElection,
        DelegertLederStateRepository delegertLederStateRepository,
        AdClient adClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        Configuration freemarkerConfig,
        MicrosoftGraphClient microsoftGraphClient
    ) {
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.leaderElection = leaderElection;
        this.delegertLederStateRepository = delegertLederStateRepository;
        this.adClient = adClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.freemarkerConfig = freemarkerConfig;
        this.microsoftGraphClient = microsoftGraphClient;
    }


    @Scheduled(cron = "0 0 19 * * ?") // every day at 18:00
    public void sendEmailIfNecessary() {
        // await until initial load is complete
        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        if (!leaderElection.isLeader()) {
            return;
        }

        log.info("Leader pod: Preparing to send email about expiring delegert leder");
        delegertLederStateRepository.getAll()
            .stream()
            .filter(it -> expiresInExactlySpecifiedWeeks(it, 5) || expiresInExactlySpecifiedWeeks(it, 2))
            .forEach(this::sendEmail);
    }

    private void sendEmail(LederTilgangState expiringDelegering) {
        try {
            List<String> recipientNavidents = List.of(expiringDelegering.av(), expiringDelegering.til());
            var adRessurser = adClient.fetchInAdRessursInBatches(recipientNavidents);

            var lederAdRessurs = adRessurser.get(expiringDelegering.av());
            var delegertLederAdRessurs = Optional.ofNullable(adRessurser.get(expiringDelegering.til()));
            var delegertLederNavn = delegertLederAdRessurs.map(AdRessurs::getDisplayName).orElse("Ukjent Navn");

            if (lederAdRessurs == null) {
                log.error(
                    "Sending email-notification about future expiry of LederTilgangState {} failed. Email address of leder not retrieved. ",
                    expiringDelegering.fid()
                );
                return;
            } else {
                log.debug("Preparing to send email-notification about future expiry of LederTilgangState {}", expiringDelegering.fid());
            }
            String subject = String.format(
                "Delegert tilgang: Varsel før utløp for %s (%s), orgenhet %s",
                delegertLederNavn, expiringDelegering.til(), expiringDelegering.orgenhetId()
            );
            var emailTemplateVariables = new EmailTemplateVariables(expiringDelegering, delegertLederNavn);
            var emailBody = generateEmailBody(emailTemplateVariables);
            if (emailBody.isBlank()) {
                log.warn("Epost body er tom, Varsel før utløpsdato for ressurs vil ikke bli sendt");
                return;
            }

            var recipientList = List.of(lederAdRessurs.getEmail());
            var delegertLederEmail = delegertLederAdRessurs.map(AdRessurs::getEmail).orElse(null);
            List<String> ccRecipientList = delegertLederEmail != null ? List.of(lederAdRessurs.getEmail()) : List.of();
            if (delegertLederEmail == null) {
                log.warn(
                    "Sending email-notification about future expiry of LederTilgangState {} mishap. CC email address not retrieved",
                    expiringDelegering.fid()
                );
            }

            var message = new EmailMessage(subject, emailBody, recipientList, ccRecipientList);
            microsoftGraphClient.sendEmail(message).subscribe();
            log.info("Sent email-notification about future expiry of LederTilgangState {}", expiringDelegering.fid());
        } catch (ADResponseException e) {
            throw new TilgangDelegertEmailException("TilgangDelegert: Fant ikke epost adresse for ressurs");
        } catch (IOException | TemplateException e) {
            throw new TilgangDelegertEmailException("TilgangDelegert: Feil ved generering av epost innhold");
        }
    }


    private boolean expiresInExactlySpecifiedWeeks(LederTilgangState state, int weeks) {
        return state.isActive() && state.gyldigTom().minusWeeks(weeks).equals(LocalDate.now());
    }


    private String generateEmailBody(EmailTemplateVariables emailTemplateVariables) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        freemarkerConfig.getTemplate(DELEGERT_TILGANG_UTLOEPER_TEMPLATE).process(emailTemplateVariables, stringWriter);
        return stringWriter.getBuffer().toString();
    }


    private class EmailTemplateVariables extends HashMap<String, String> {
        EmailTemplateVariables(LederTilgangState lederTilgangState, String undersottName) {
            super();
            var orgEnhetNavn = orgEnhetStateRepository.findById(lederTilgangState.orgenhetId()).map(OrgEnhetState::navn).orElse("Ukjent");
            this.putAll(Map.of(
                "delegertRessursDisplayname", undersottName,
                "delegertRessursNavIdent", lederTilgangState.til(),
                "orgenhetNavn", orgEnhetNavn,
                "orgenhetId", lederTilgangState.orgenhetId(),
                "tomDato", DateUtils.formatDateTime(lederTilgangState.gyldigTom())
            ));

        }
    }

    public static class TilgangDelegertEmailException extends RuntimeException {
        public TilgangDelegertEmailException(String message) {
            super(message);
        }
    }


}
