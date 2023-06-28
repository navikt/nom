package no.nav.nom.apps.api.ressurs;

import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RessursOpprettetEmailService {

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);

    private final EmailClient emailClient;
    private final AdClient adClient;
    private final no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService orgtilknytningService;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursService;

    public RessursOpprettetEmailService(
        EmailClient emailClient,
        AdClient adClient,
        no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService orgtilknytningService,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursStateRepository ressursService
    ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.orgtilknytningService = orgtilknytningService;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursService = ressursService;
    }

    public void notifyByEmail(RessursState opprettetRessurs) {

        if (opprettetRessurs == null) {
            log.debug("Epostvarsling om ressursopprettelse feilet. opprettetRessurs parameter er null");
            return;
        }

        var recipientNavidentList = orgtilknytningService.getOrgtilknytningerByNavIdent(opprettetRessurs.navident())
            .stream()
            .map(kobling -> orgEnhetStateRepository.findById(kobling.orgEnhetId())
                .flatMap(OrgEnhetState::lederFid)
                .flatMap(fid -> ressursService.findByFid(UUID.fromString(fid)))
                .map(ressursState -> ressursState.navident())
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (recipientNavidentList.isEmpty()) {
            log.debug("Epostvarsling feilet. Fant ikke leder for opprettet ressurs {}", opprettetRessurs.navident());
        }

        log.info("Sender epostvarsling om opprettelse av ressurs{} til {}", opprettetRessurs.navident(), recipientNavidentList);
        composeEmail(opprettetRessurs, recipientNavidentList);
    }

    private void composeEmail(RessursState opprettetRessurs, Set<String> recipientNavidents) {

        var adRessurser = adClient.getAdRessurser(recipientNavidents);
        var recipientList = adRessurser.getRessurser().stream().map(adRessurs -> adRessurs.getEmail()).collect(Collectors.toList());

        String subject = String.format("Ressurs opprettet %s %s", opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident());
        String content = "Test " + recipientList;

        EmailMessage message = new EmailMessage(subject, content, List.of("eziz.annagurban@nav.no"));
        emailClient.sendEmail(message);
    }

}
