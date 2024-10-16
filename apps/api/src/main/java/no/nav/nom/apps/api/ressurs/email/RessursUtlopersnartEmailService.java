package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.clients.email.EmailMessage;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RessursUtlopersnartEmailService {

    private static final String SLUTTDATO_UTLOPER_VARSEL_TEMPLATE = "epostvarsel-utlopsdato.ftlh";
    private static final String UNLEASH_SLUTTDATO_UTLOPER_EPOST_VARSEL = "nom.ressurs.sluttdato_utloper_email";
    private final Logger log = LoggerFactory.getLogger(RessursUtlopersnartEmailService.class);
    private final EmailClient emailClient;
    private final AdClient adClient;
    private final RessursStateRepository ressursStateRepository;
    private final Configuration freemarkerConfig;
    private final Unleash unleash;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final LederResolver lederResolver;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final NomMastered nomMastered;

    public RessursUtlopersnartEmailService(
        EmailClient emailClient,
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        Configuration freemarkerConfig,
        Unleash unleash,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        LederResolver lederResolver,
        OrgEnhetStateRepository orgEnhetStateRepository,
        NomMastered nomMastered
    ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.freemarkerConfig = freemarkerConfig;
        this.unleash = unleash;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.lederResolver = lederResolver;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.nomMastered = nomMastered;
    }

    @Scheduled(cron = "0 0 19 * * ?") // every day at 18:00
    public void sendEmail() {
        if(unleash.isEnabled(UNLEASH_SLUTTDATO_UTLOPER_EPOST_VARSEL)) {
            log.info("Sender en e-post om utløpsdato for ressurser til ledere av organisasjonsenheter");
            var ressurserUtloperSnart = getUtlopendeRessurser();
            log.info("Ressurser utløper snart: {}", ressurserUtloperSnart);
            if(!ressurserUtloperSnart.isEmpty()){
                ressurserUtloperSnart.forEach(this::sendEmail);
            }
        }
    }

    private void sendEmail(UtloperRessursData utlopendeRessurs) {
        if (!nomMastered.isMastered(utlopendeRessurs.ressursNavident())) {
            log.warn("Navidenten er ikke mastret i NOM. Epost om utløpende ressurs vil ikke bli sendt");
            return;
        }
        try {
            Set<String> recipientNavidents = new HashSet<>(utlopendeRessurs.orgenhetData().values());
            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();
            log.info("Sender epose varsel om utløpende ressurs: {}({}), til navidents og eposts: {}, {}"
                ,utlopendeRessurs.ressurNavn(), utlopendeRessurs.ressursNavident(), recipientNavidents, recipientList);

            String subject = String.format("Varsel før utløpsdato for %s (%s)", utlopendeRessurs.ressurNavn(), utlopendeRessurs.ressursNavident());
            String emailBody = generateEmailBody(utlopendeRessurs);
            if(emailBody.isBlank()) {
                log.warn("Epost body er tom, Varsel før utløpsdato for ressurs vil ikke bli sendt");
                return;
            }

            log.info("Sender ressurs sluttdato utløper epost subject: {}, recipients: {}, message: {}", subject, recipientList, emailBody);
            /**
             * NOTE: for the time being, we will only send to Line <Line.Eeg-Larsen@nav.no>
             * later we will clean this up and send to all leaders of the orgenhet
             * EmailMessage message = new EmailMessage(subject, body, recipientList);
             */
            EmailMessage message = new EmailMessage(subject, emailBody, List.of("Line.Eeg-Larsen@nav.no"));
            emailClient.sendEmail(message).subscribe();
        } catch (ADResponseException e) {
            throw new RessursOpprettetEmailException("UtloperRessurs: Fant ikke epost adresse for ressurs");
        } catch (IOException | TemplateException e) {
            throw new RessursOpprettetEmailException("UtloperRessurs: Feil ved generering av epost innhold");
        }
    }

    private String generateEmailBody(UtloperRessursData utlopendeRessurs) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "orgenhetNavn", utlopendeRessurs.orgenhetData().keySet().stream().findFirst().orElse("Ukjent"),
            "ressurNavnOgNavIdent", utlopendeRessurs.ressurNavn() + "(" + utlopendeRessurs.ressursNavident() + ")" ,
            "ressursSluttdato", DateUtils.formatDateTime(utlopendeRessurs.ressursSluttdato()),
            "ressursSluttdatoPlus1dag", DateUtils.formatDateTime(utlopendeRessurs.ressursSluttdato().plusDays(1))
        );
        freemarkerConfig.getTemplate(SLUTTDATO_UTLOPER_VARSEL_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    public List<UtloperRessursData> getUtlopendeRessurser() {
        return ressursStateRepository.findAllActiveRessurser().values().stream()
            .filter(ressurs -> ressurs.sluttdato().isPresent())
            .filter(ressurs -> ressurs.sluttdato().get().isEqual(LocalDate.now().plusWeeks(2)) ||
                ressurs.sluttdato().get().isEqual(LocalDate.now().plusWeeks(5)))
            .map(ressurs -> {
                var visningsnavn = ressurs.visningsnavn() != null? ressurs.visningsnavn().getFormattedVisningsnavn() : "Ukjent";
                var orgTilknytninger = orgtilknytningStateRepository.findByNavident(ressurs.navident());

                // innholder orgenhetNavn og orgenhetLederNavident
                Map<String, String> orgenhetData = orgTilknytninger.stream()
                    .filter(orgtilknytningState -> orgtilknytningState.gyldigTom().isPresent() && orgtilknytningState.gyldigTom().get().isEqual(ressurs.sluttdato().get()))
                    .map(orgtilknytningState -> {
                        var orgenhetState = orgEnhetStateRepository.findById(orgtilknytningState.orgEnhetId());
                        String orgenhetNavn = orgenhetState.isPresent() ? orgenhetState.get().navn() : "Ukjent";
                        var leader =  lederResolver.leder(
                            // populate at least agressoId, orgNiv, and navn
                            OrgEnhet.builder()
                                .id(orgenhetState.get().id())
                                .navn(orgenhetState.get().navn())
                                .agressoId(orgenhetState.get().agressoId().orElse(null))
                                .orgNiv(orgenhetState.get().agressoOrgNivaa().orElse(null))
                                .build()
                        );
                        if(!leader.isEmpty()){
                            return Map.entry(orgenhetNavn, leader.get(0).getRessurs().getNavIdent());
                        }
                        return Map.entry(orgenhetNavn, "Ukjent");
                    })
                    .filter(entry -> !entry.getValue().equals("Ukjent"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                return UtloperRessursData.builder()
                    .ressurNavn(visningsnavn)
                    .ressursNavident(ressurs.navident())
                    .ressursSluttdato(ressurs.sluttdato().get())
                    .orgenhetData(orgenhetData)
                    .build();
            }).toList();
    }


}
