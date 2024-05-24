package no.nav.nom.apps.api.ressurs.email;

import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RessursOpprettetEmailService {

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);

    private final EmailClient emailClient;
    private final AdClient adClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final LederResolver lederResolver;

    public RessursOpprettetEmailService(
        EmailClient emailClient,
        AdClient adClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursStateRepository ressursStateRepository,
        DvhRepository dvhRepository,
        LederResolver lederResolver
    ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.lederResolver = lederResolver;
    }

    public void notifyByEmail(String navident, String orgenhetId) {
        log.info("Sender epost om opprettelse av ressurs {} til leder av orgenhet {}", navident, orgenhetId);

        var opprettetRessurs = ressursStateRepository.findByNavident(navident)
            .orElseThrow(() -> new RessursOpprettetEmailException("Opprettet ressurs mangler i state"));
        var orgEnhet = orgEnhetStateRepository.findById(orgenhetId)
            .orElseThrow(() -> new RessursOpprettetEmailException("Fant ikke orgenhet med gitt id"));

        // get leaders using lederResolver
        List<OrgEnhetsLeder> leaders = lederResolver.leder(OrgEnhet.builder()
                .agressoId(orgEnhet.agressoId().get())
                .orgNiv(orgEnhet.agressoOrgNivaa().get())
                .navn(orgEnhet.navn())
                .build());

        // throw exception if no leaders are found
        if(leaders.isEmpty()) {
            throw new RessursOpprettetEmailException("Fant ikke leder for orgenhet");
        }

        // else send email to all leaders
        var orgEnhetsLeders = leaders.stream()
            .map(OrgEnhetsLeder::getRessurs)
            .map(Ressurs::getNavident)
            .collect(Collectors.toSet());
        composeAndSendEmail(opprettetRessurs, orgEnhetsLeders, orgEnhet.navn());
    }

    private void composeAndSendEmail(RessursState opprettetRessurs, Set<String> recipientNavidents, String orgenhetName) {
        try {
            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

            String subject = String.format("Ressurs opprettet %s (%s)",
                opprettetRessurs.visningsnavn().getFormattedVisningsnavn(), opprettetRessurs.navident()
            );
            String body = getFormattetEmailBody(
                opprettetRessurs.visningsnavn().getFormattedVisningsnavn(),
                opprettetRessurs.navident(),
                orgenhetName
            );

            //TODO: extra logging, for testing purposes. Remove this after emailing is enabled
            log.info("Sender epost subject: {} recipients: {}", subject, recipientList);

            EmailMessage message = new EmailMessage(subject, body, recipientList);
            emailClient.sendEmail(message);

        } catch (ADResponseException e) {
            throw new RessursOpprettetEmailException("Fant ikke leder epost adresse for leder");
        }
    }

    private String getFormattetEmailBody(String resursName, String navIdent, String orgenhetName) {
        return String.format(
            "Hei!\n" +
            "\n" +
            "%s er nå blitt opprettet som medarbeider i NOM, og blitt tildelt NAV-ident: %s. Personen er blitt tilknyttet %s.\n" +
            "\n" +
            "Du finner informasjon om personen på https://nom.nav.no/ressurs/%s\n" +
            "\n" +
            "Du mottar denne e-posten fordi du har opprettet en medarbeider i NOM, eller fordi du er leder for en medarbeider som er blitt opprettet i NOM.\n" +
            "\n" +
            "For spørsmål eller tilbakemeldinger, kontakt oss: mugunthan.dharmadas@nav.no;line.eeg-larsen@nav.no. NOM er i pilotfase, og både ris og ros gir oss verdifulle tilbakemeldinger. Ved feil, kan også Porten benyttes: https://jira.adeo.no/plugins/servlet/desk/portal/581/create/5166.\n" +
            "\n" +
            "Dette er en autogenerert e-post fra NAV organisasjonsmaster (NOM - https://nom.nav.no/). Du kan ikke svare på denne e-posten.\n" +
            "\n" +
            "__", resursName, navIdent, orgenhetName, navIdent);
    }

}


