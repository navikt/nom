package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.EmailSendingUnforeseenException;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;
import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Component
public class RessursUtloperSnartEmailService {

    private static final String SLUTTDATO_UTLOPER_VARSEL_TEMPLATE = "epostvarsel-utlopsdato.ftlh";
    private static final String SLUTTDATO_UTLOPER_VARSEL_FOR_ANSATT_TEMPLATE = "epostvarsel-utlopsdato-ansatt.ftlh";
    private final Logger log = LoggerFactory.getLogger(RessursUtloperSnartEmailService.class);
    private final MicrosoftGraphClient microsoftGraphClient;
    private final AdClient adClient;
    private final RessursStateRepository ressursStateRepository;
    private final Configuration freemarkerConfig;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final NomMastered nomMastered;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final LeaderElection leaderElection;
    private final OrgEnhetLederService orgEnhetLederService;
    private final Unleash unleash;

    public RessursUtloperSnartEmailService(
        MicrosoftGraphClient microsoftGraphClient,
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        Configuration freemarkerConfig,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        NomMastered nomMastered,
        DvhStartupLoadMonitor dvhStartupLoadMonitor,
        LeaderElection leaderElection,
        OrgEnhetLederService orgEnhetLederService,
        Unleash unleash
    ) {
        this.microsoftGraphClient = microsoftGraphClient;
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.freemarkerConfig = freemarkerConfig;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.nomMastered = nomMastered;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.leaderElection = leaderElection;
        this.orgEnhetLederService = orgEnhetLederService;
        this.unleash = unleash;
    }

    @Scheduled(cron = "0 0 18 * * ?") // every day at 18:00
    public void sendEmail() {
        // await until initial load is complete
        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        // skip if not leader
        if (!leaderElection.isLeader()) {
            log.info("This pod is not the leader, skipping email sending about expiring ressurses");
            return;
        }

        log.info("Sender en e-post om utløpsdato for ressurser til ledere av organisasjonsenheter");
        var ressurserUtloperSnart = getUtlopendeRessurser();
        log.info(
            "Totalt antall ressurser som utløper snart: {} Ressurser: {}",
            ressurserUtloperSnart.size(),
            ressurserUtloperSnart.stream().map(RessursUtloperData::ressursNavident).toList());

        var ressursUtloperOmFemEllerToUker = ressurserUtloperSnart.stream()
            .filter(ressursUtloperData -> ressursUtloperData.orgenhetLederNavident() != null)
            .toList();

        var ressursUtloperOmToUker = ressurserUtloperSnart.stream()
            .filter(ressursUtloperData -> ressursUtloperData.ressursSluttdato() != null)
            .filter(ressursUtloperData -> ressursUtloperData.ressursSluttdato().isEqual(LocalDate.now().plusWeeks(2)))
            .toList();

        log.info(
            "Ressurser utløper snart med leder navident, antall: {}, ressurser: {}",
            ressurserUtloperSnart.size(),
            ressurserUtloperSnart.stream().map(RessursUtloperData::ressursNavident).toList()
        );
        if (!ressursUtloperOmFemEllerToUker.isEmpty()) {
            ressursUtloperOmFemEllerToUker.forEach(this::sendEmail);
            if (unleash.isEnabled("nom.sluttdato-epost-ansatte")) {
                ressursUtloperOmToUker.forEach(this::sendEmailTilAnsatt);
            }
        }
    }

    private void sendEmail(RessursUtloperData utlopendeRessurs) {
        if (!nomMastered.isMastered(utlopendeRessurs.ressursNavident())) {
            log.warn("Navidenten er ikke mastret i NOM. Epost om utløpende ressurs vil ikke bli sendt");
            return;
        }
        try {
            Set<String> recipientNavidents = new HashSet<>();
            recipientNavidents.add(utlopendeRessurs.orgenhetLederNavident());

            if (isNotEmpty(utlopendeRessurs.sistEndretAvNavidenter())) {
                recipientNavidents.addAll(utlopendeRessurs.sistEndretAvNavidenter());
            }

            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();
            log.info(
                TEAM_LOGS, "Sender epost varsel om utløpende ressurs: {}({}), til navidents og eposts: {}, {}",
                utlopendeRessurs.ressurNavn(), utlopendeRessurs.ressursNavident(), recipientNavidents, recipientList
            );

            String subject = String.format("Varsel før utløpsdato for %s (%s)", utlopendeRessurs.ressurNavn(), utlopendeRessurs.ressursNavident());
            String emailBody = generateEmailBody(utlopendeRessurs);
            if (emailBody.isBlank()) {
                log.warn("Epost body er tom, Varsel før utløpsdato for ressurs vil ikke bli sendt");
                return;
            }

            log.info(TEAM_LOGS, "Sender ressurs sluttdato utløper epost subject: {}, recipients: {}, message: {}", subject, Stream.concat(recipientList.stream(), bccRecipientsEmailAddresses().stream()).collect(Collectors.toSet()), emailBody);
            EmailMessage message = new EmailMessage(subject, emailBody, recipientList, List.of(), bccRecipientsEmailAddresses());
            microsoftGraphClient.sendEmail(message).subscribe();
        } catch (ADResponseException e) {
            EmailSendingUnforeseenException.emailNotFound(utlopendeRessurs.orgenhetLederNavident());
        } catch (IOException | TemplateException e) {
            throw new RessursOpprettetEmailException("UtloperRessurs: Feil ved generering av epost innhold");
        }
    }

    private void sendEmailTilAnsatt(RessursUtloperData utlopendeRessurs) {
        if (!nomMastered.isMastered(utlopendeRessurs.ressursNavident())) {
            log.warn("Navidenten er ikke mastret i NOM. Epost om utløpende ressurs vil ikke bli sendt");
            return;
        }
        try {
            Set<String> recipientNavidents = Set.of(utlopendeRessurs.ressursNavident());
            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

            String subject = "Din sluttdato nærmer seg";
            String emailBody = generateEmailBodyForAnsatt(utlopendeRessurs);
            if (emailBody.isBlank()) {
                log.warn("Epost body er tom, Varsel før utløpsdato for ressurs vil ikke bli sendt");
                return;
            }
            EmailMessage message = new EmailMessage(subject, emailBody, recipientList, List.of(), bccRecipientsEmailAddresses());
            microsoftGraphClient.sendEmail(message).subscribe();
        } catch (ADResponseException e) {
            EmailSendingUnforeseenException.emailNotFound(utlopendeRessurs.ressursNavident());
        } catch (IOException | TemplateException e) {
            throw new EmailSendingUnforeseenException("UtloperRessurs: Feil ved generering av epost innhold");
        }
    }

    private String generateEmailBody(RessursUtloperData utlopendeRessurs) throws IOException, TemplateException {
        var stringWriter = new StringWriter();

        var orgenhetNavn = utlopendeRessurs.orgenhetNavn();
        var ressursNavnOgNavIdent = utlopendeRessurs.ressurNavn() + "(" + utlopendeRessurs.ressursNavident() + ")";
        var ressursSluttdato = DateUtils.formatDateTime(utlopendeRessurs.ressursSluttdato());
        var ressursSluttdatoPlus1dag = DateUtils.formatDateTime(utlopendeRessurs.ressursSluttdato().plusDays(1));
        var ressursNavn = utlopendeRessurs.ressurNavn();
        log.info(
            TEAM_LOGS,
            "Genererer epost varsel body om utløpsdato for ressurs: {}, {}, {}, {}, {}",
            orgenhetNavn,
            ressursNavn,
            ressursNavnOgNavIdent,
            ressursSluttdato,
            ressursSluttdatoPlus1dag
        );

        var model = Map.of(
            "orgenhetNavn", orgenhetNavn,
            "ressursNavn", ressursNavn,
            "ressurNavnOgNavIdent", ressursNavnOgNavIdent,
            "ressursSluttdato", ressursSluttdato,
            "ressursSluttdatoPlus1dag", ressursSluttdatoPlus1dag
        );
        freemarkerConfig.getTemplate(SLUTTDATO_UTLOPER_VARSEL_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    private String generateEmailBodyForAnsatt(RessursUtloperData utlopendeRessurs) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var ressursSluttdato = DateUtils.formatDateTime(utlopendeRessurs.ressursSluttdato());
        var ressursSluttdatoPlus1dag = DateUtils.formatDateTime(utlopendeRessurs.ressursSluttdato().plusDays(1));
        var ressursNavn = utlopendeRessurs.ressurNavn();
        log.info(
            TEAM_LOGS,
            "Genererer epost varsel body om utløpsdato for ressurs: {}, {}, {}",
            ressursNavn,
            ressursSluttdato,
            ressursSluttdatoPlus1dag
        );

        var model = Map.of(
            "ressursNavn", ressursNavn,
            "ressursSluttdato", ressursSluttdato,
            "ressursSluttdatoPlus1dag", ressursSluttdatoPlus1dag
        );
        freemarkerConfig.getTemplate(SLUTTDATO_UTLOPER_VARSEL_FOR_ANSATT_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    public List<RessursUtloperData> getUtlopendeRessurser() {
        return ressursStateRepository.findAllActiveRessurser().values().stream()
            .filter(RessursUtloperSnartEmailService::ressursMedUtlopsDatoOmFemEllerToUker)
            .map(this::finnOrgEnhetOgLederTilRessurs)
            .toList();
    }

    private RessursUtloperData finnOrgEnhetOgLederTilRessurs(RessursState ressurs) {
        try {
            var dolOrgtilknytning = orgtilknytningStateRepository.findByNavident(ressurs.navident()).stream()
                .filter(OrgtilknytningState::erDagligOppfolging)
                .filter(ot -> ot.gyldigTom() != null)
                .filter(ot -> DateUtils.isPeriodActive(ot.gyldigFom(), ot.gyldigTom()) || ot.gyldigFom().isAfter(LocalDate.now()))
                .filter(ot -> isRessursSluttdatoEqualToOrgtilknytningGyldigTom(ressurs, ot))
                .findFirst()
                .orElse(null);

            Set<String> sistEndretAvNavidenter = new HashSet<>();

            leggTilSetHvisNavidentOgIkkeRessursenSelv(ressurs.sistEndretAv(), ressurs.navident(), sistEndretAvNavidenter);
            if (dolOrgtilknytning != null) {
                leggTilSetHvisNavidentOgIkkeRessursenSelv(dolOrgtilknytning.sistEndretAv(), ressurs.navident(), sistEndretAvNavidenter);
            }

            var utloperRessursData = RessursUtloperData.builder()
                .ressurNavn(ressurs.visningsnavn().getFormattedVisningsnavn())
                .ressursNavident(ressurs.navident())
                .ressursSluttdato(ressurs.sluttdato())
                .sistEndretAvNavidenter(sistEndretAvNavidenter)
                .build();

            if (isNull(dolOrgtilknytning)) {
                log.warn("ALERT: Fant ikke gyldig orgtilknytning for ressurs: {}", ressurs.navident());
                return utloperRessursData;
            }

            var orgEnhetState = orgEnhetStateRepository.findById(dolOrgtilknytning.orgEnhetId()).orElse(null);

            if (isNull(orgEnhetState)) {
                log.warn("ALERT: Fant ikke orgenhet med id: {}", dolOrgtilknytning.orgEnhetId());
                return utloperRessursData;
            }

            var leder = orgEnhetLederService.getAktivLederForOrgEnhet(orgEnhetState.id());

            if (leder.isEmpty()) {
                log.warn("ALERT: Fant ikke leder for orgenhet: {}, ressurs: {}", orgEnhetState.navn(), ressurs.navident());
                return utloperRessursData.toBuilder().orgenhetNavn(orgEnhetState.navn()).build();
            }

            return utloperRessursData.toBuilder()
                .orgenhetNavn(orgEnhetState.navn())
                .orgenhetLederNavident(leder.get().navident())
                .build();

        } catch (Exception e) {
            log.error("ALERT: En feil skjedde under henting av ressurs med fem eller to uker til utløpsdato: {}\n", ressurs.navident(), e);
            return RessursUtloperData.builder()
                .ressurNavn(ressurs.visningsnavn().getFormattedVisningsnavn())
                .ressursNavident(ressurs.navident())
                .build();
        }
    }

    private void leggTilSetHvisNavidentOgIkkeRessursenSelv(String sistEndretAv, String ressursNavident, Set<String> mottakere) {
        if (sistEndretAv == null || sistEndretAv.isBlank()) {
            return;
        }
        ressursStateRepository.findByNavident(sistEndretAv)
            .filter(r -> !r.navident().equals(ressursNavident)) // Ikke ta med hvis det er ressursen selv som har gjort endring
            .filter(r -> isActive(r.startdato(), r.sluttdato())) // Er ressursen som gjorde sist endring aktiv?
            .map(RessursState::navident)
            .ifPresent(mottakere::add);
    }

    private static boolean ressursMedUtlopsDatoOmFemEllerToUker(RessursState ressurs) {
        return ressurs.sluttdato() != null && (ressurs.sluttdato().isEqual(LocalDate.now().plusWeeks(2)) ||
            ressurs.sluttdato().isEqual(LocalDate.now().plusWeeks(5)));
    }

    private static boolean isRessursSluttdatoEqualToOrgtilknytningGyldigTom(RessursState ressurs, OrgtilknytningState orgtilknytningState) {
        return orgtilknytningState.gyldigTom() != null
            && ressurs.sluttdato() != null
            && orgtilknytningState.gyldigTom().isEqual(ressurs.sluttdato());
    }

    private List<String> bccRecipientsEmailAddresses() {
        return List.of();
    }
}
