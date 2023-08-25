package no.nav.nom.apps.api.ressurs.email;

import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class RessursOpprettetEmailService {

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);

    private final EmailClient emailClient;
    private final AdClient adClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursService;

    public RessursOpprettetEmailService(
        EmailClient emailClient,
        AdClient adClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursStateRepository ressursService
    ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursService = ressursService;
    }

    public void notifyByEmail(String navident, String orgenhetId) {
        log.info("Sender epost om opprettelse av ressurs {} til leder av orgenhet {}", navident, orgenhetId);

        var opprettetRessurs = ressursService.findByNavident(navident)
            .orElseThrow(() -> new RessursOpprettetEmailException("Opprettet ressurs mangler i state"));
        var orgEnhet = orgEnhetStateRepository.findById(orgenhetId)
            .orElseThrow(() -> new RessursOpprettetEmailException("Fant ikke orgenhet med gitt id"));

        var lederNavidentOpt = ressursService.findByFid(UUID.fromString(orgEnhet.fid()))
            .map(RessursState::navident);

        lederNavidentOpt.ifPresentOrElse(
            lederNavident -> composeAndSendEmail(opprettetRessurs, Set.of(lederNavident)),
            () -> {throw new RessursOpprettetEmailException("Fant ikke leder for orgenhet");}
        );
    }

    private void composeAndSendEmail(RessursState opprettetRessurs, Set<String> recipientNavidents) {

        try {
            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

            String subject = String.format("Ressurs opprettet %s (%s)",
                opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident()
            );
            String body = String.format("Ressurs opprettet %s (%s) f",
                opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident()
            );

            //TODO: extra logging, for testing purposes. Remove this after emailing is enabled
            log.info("Sender epost subject: {} recipients: {}", subject, recipientList);

            EmailMessage message = new EmailMessage(subject, body, recipientList);
            emailClient.sendEmail(message);

        } catch (ADResponseException e) {
            throw new RessursOpprettetEmailException("Fant ikke leder epost adresse for leder");
        }
    }

}
