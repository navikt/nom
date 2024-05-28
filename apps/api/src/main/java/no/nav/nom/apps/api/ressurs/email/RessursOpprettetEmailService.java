package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.TemplateException;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.exception.LederNotFoundException;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import freemarker.template.Configuration;


import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RessursOpprettetEmailService {

    private static final String OPPRETTELSE_VARSEL_TEMPLATE = "opprettelse-varsel.ftlh";

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);

    private final EmailClient emailClient;
    private final AdClient adClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final DvhRepository dvhRepository;
    private final Configuration freemarkerConfig;

    public RessursOpprettetEmailService(
        EmailClient emailClient,
        AdClient adClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursStateRepository ressursStateRepository,
        DvhRepository dvhRepository,
        Configuration freemarkerConfig
        ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.dvhRepository = dvhRepository;
        this.freemarkerConfig = freemarkerConfig;
    }

    public void notifyByEmail(String navident, String orgenhetId) {
        log.info("Sender epost om opprettelse av ressurs {} til leder av orgenhet {}", navident, orgenhetId);

        var opprettetRessurs = ressursStateRepository.findByNavident(navident)
            .orElseThrow(() -> new RessursOpprettetEmailException("Opprettet ressurs mangler i state"));
        var orgEnhet = orgEnhetStateRepository.findById(orgenhetId)
            .orElseThrow(() -> new RessursOpprettetEmailException("Fant ikke orgenhet med gitt id"));

        // get leaders using lederResolver
        List<String> leaders = dvhRepository.getLederByAgressoIdAndNivAndNavn(orgEnhet.agressoId().get(), orgEnhet.agressoOrgNivaa().get(), orgEnhet.navn())
            .stream()
            .map(leder -> tilOrgEnhetsLederNavIdents(ressursStateRepository.findByNavident(leder.getNavIdent(), MappingUtils::toRessurs)
                .orElseThrow(LederNotFoundException::new)))
            .toList();

        // throw exception if no leaders are found
        if(leaders.isEmpty()) {
            throw new RessursOpprettetEmailException("Fant ikke leder for orgenhet " + orgenhetId);
        }

        // else send email to all leaders
        composeAndSendEmail(opprettetRessurs, Set.copyOf(leaders), orgEnhet.navn());
    }

    private String tilOrgEnhetsLederNavIdents(Ressurs leder) {
        return leder.getNavIdent();
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
        } catch (IOException | TemplateException e) {
            throw new RessursOpprettetEmailException("Feil ved generering av epost innhold");
        }
    }

    public String getFormattetEmailBody(String resursName, String navIdent, String orgenhetName) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "resursName", resursName,
            "navIdent", navIdent,
            "orgenhetName", orgenhetName);
        freemarkerConfig.getTemplate(OPPRETTELSE_VARSEL_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();

        /*
        return String.format(
                """
                        Hei!

                        %s er nå blitt opprettet som medarbeider i NOM, og blitt tildelt NAV-ident: %s. Personen er blitt tilknyttet %s.

                        Du finner informasjon om personen på https://nom.nav.no/ressurs/%s

                        Du mottar denne e-posten fordi du har opprettet en medarbeider i NOM, eller fordi du er leder for en medarbeider som er blitt opprettet i NOM.

                        For spørsmål eller tilbakemeldinger, kontakt oss (mugunthan.dharmadas@nav.no;line.eeg-larsen@nav.no). NOM er i pilotfase, og både ris og ros gir oss verdifulle tilbakemeldinger. Ved feil, kan også Porten benyttes: https://jira.adeo.no/plugins/servlet/desk/portal/581/create/5166.

                        Dette er en autogenerert e-post fra NAV organisasjonsmaster (NOM). Du kan ikke svare på denne e-posten.

                        __""", resursName, navIdent, orgenhetName, navIdent);
         */
    }

}


