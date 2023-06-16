package no.nav.nom.apps.api.ressurs;

import no.nav.nom.apps.api.email.EmailClient;
import no.nav.nom.apps.api.email.EmailMessage;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RessursOpprettetEmailService {

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);

    private final EmailClient emailClient;
    private final no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService orgtilknytningService;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursStateRepository;

    public RessursOpprettetEmailService(
        EmailClient emailClient,
        no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService orgtilknytningService,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursStateRepository ressursStateRepository
    ) {
        this.emailClient = emailClient;
        this.orgtilknytningService = orgtilknytningService;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursStateRepository = ressursStateRepository;
    }

    public void notifyByEmail(String opprettetRessursNavident) {

        var opprettetRessurs = ressursStateRepository.findByNavident(opprettetRessursNavident);
        var recipientList = orgtilknytningService.getOrgtilknytningerByNavIdent(opprettetRessursNavident)
            .stream()
            .map(kobling -> orgEnhetStateRepository
                .findById(kobling.orgEnhetId())
                .flatMap(OrgEnhetState::lederFid)
                .flatMap(fid -> ressursStateRepository.findByFid(UUID.fromString(fid)))
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if(opprettetRessurs.isPresent() && !recipientList.isEmpty()){
            composeEmail(opprettetRessurs.get(), recipientList);
        } else {
            log.warn("Kunne ikke varsle via epost om ressursopprettelse av {}", opprettetRessursNavident);
        }
    }

    private void composeEmail(RessursState opprettetRessurs, List<RessursState> recipients) {
        var emailList = recipients.stream().map(ressurs -> ressurs.navident() + "@nav.no").toList();

        String subject = String.format("Ressurs opprettet %s %s",
            opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident());
        String content = "Test " + emailList;

        EmailMessage message = new EmailMessage(subject, content, List.of("eziz.annagurban@nav.no"));
        emailClient.sendEmail(message);
    }

}
