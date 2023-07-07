package no.nav.nom.apps.api.ressurs;

import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
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

        var opprettetRessurs = ressursService.findByNavident(navident);
        var orgEnhet = orgEnhetStateRepository.findById(orgenhetId);

        if (opprettetRessurs.isEmpty() || orgenhetId.isEmpty()) {
            //TODO: IllegalArgumentException
            log.debug("Epostvarsling om ressursopprettelse feilet. opprettetRessurs parameter er null");
            return;
        }

        var lederNavidentOpt = orgEnhet
            .flatMap(OrgEnhetState::lederFid)
            .flatMap(lederFid -> ressursService.findByFid(UUID.fromString(lederFid)))
            .map(RessursState::navident);

        lederNavidentOpt.ifPresentOrElse(
            lederNavident -> {
                log.info("Sender epostvarsling om opprettelse av ressurs{} til {}", navident, lederNavident);
                composeEmail(opprettetRessurs.get(), Set.of(lederNavident));
            },
            () -> log.debug("Epostvarsling feilet. Fant ikke leder for opprettet ressurs {}", navident)
        );
    }

    private void composeEmail(RessursState opprettetRessurs, Set<String> recipientNavidents) {

        var adRessurser = adClient.getAdRessurser(recipientNavidents);
        var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

        String subject = String.format("Ressurs opprettet %s (%s)",
            opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident());
        String content = "Text " + recipientList;

        EmailMessage message = new EmailMessage(subject, content, List.of("eziz.annagurban@nav.no"));
        //TODO:
        //emailClient.sendEmail(message);
    }

}
