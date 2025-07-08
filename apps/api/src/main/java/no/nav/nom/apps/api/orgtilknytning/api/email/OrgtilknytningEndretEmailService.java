package no.nav.nom.apps.api.orgtilknytning.api.email;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.common.cache.CacheQueueService;
import no.nav.nom.apps.api.common.cache.CacheType;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrgtilknytningEndretEmailService {
    private static final String OT_ENDRINGER_VARSEL_TEMPLATE = "ot-endringer-varsel.ftlh";
    public static final String MASSEFLYTTING_VARSEL_FTLH = "masseflytting-varsel.ftlh";
    public final String epost_body_item_template = "{0}({1}), fra og med {2}, til og med {3}. Endring utført av {4}, på {5}.";
    public final String epost_body_item_template_ot_notfound = "Endring({0}) utført av {1}, på {2}.";
    private final Logger log = LoggerFactory.getLogger(OrgtilknytningEndretEmailService.class);
    private final MicrosoftGraphClient microsoftGraphClient;
    private final Configuration freemarkerConfig;
    private final AdClient adClient;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final LederResolver lederResolver;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final CacheQueueService cacheQueueService;
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final LeaderElection leaderElection;
    private final NomMastered nomMastered;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgenhetService orgenhetService;
    private final Unleash unleash;
    private final String UNLEASH_SKRU_AV_EPOST_ENDRING = "nom.ot_endring_epost.skru_av_utsendelse";


    public OrgtilknytningEndretEmailService(
        MicrosoftGraphClient microsoftGraphClient,
        Configuration freemarkerConfig,
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        LederResolver lederResolver,
        CacheQueueService cacheQueueService,
        DvhStartupLoadMonitor dvhStartupLoadMonitor,
        LeaderElection leaderElection,
        NomMastered nomMastered, RemedyEnhetMapperService remedyEnhetMapperService,
        OrgenhetService orgenhetService, Unleash unleash
    ) {
        this.microsoftGraphClient = microsoftGraphClient;
        this.freemarkerConfig = freemarkerConfig;
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.lederResolver = lederResolver;
        this.cacheQueueService = cacheQueueService;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.leaderElection = leaderElection;
        this.nomMastered = nomMastered;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgenhetService = orgenhetService;
        this.unleash = unleash;
    }

    public void addToOrgtilknytningEndringerQueue(String fid, OTEndretData otEndretData) {
        // save to database
        try {
            log.info("Adding to cache: fid: {}, otEndretData: {}", fid, otEndretData);
            cacheQueueService.save(fid, otEndretData);
        } catch (IOException e) {
            // log error and ignore
            log.error("Error while saving to cache", e);
        }
    }

    public void removeFromOrgtilknytningEndringerQueue(String fid) {
        cacheQueueService.delete(fid, CacheType.OTENDRINGER.toString());
    }

    @Scheduled(cron = "0 0 18 * * ?") // every day at 18:00
    public void sendOTEndretEmail() {
        // await until initial load is complete
        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        // skip if not leader
        if (!leaderElection.isLeader()) {
            log.info("This pod is not the leader, skipping email sending about changes in Orgtilknytning");
            return;
        }

        Map<String, List<OTEndretData>> otEndringerByFid = cacheQueueService.getOTEndringerFromCache();
        //Note: print the queue for debugging. To be removed later
        log.info("otEndringerByFid size, keys, values: {}, {}, {}", otEndringerByFid.size(), otEndringerByFid.keySet(), otEndringerByFid.values());

        // as OT changes are per orgenhet, group them by navident
        Map<String, List<OTEndretData>> otEndringerByNavident =  new HashMap<>();
        otEndringerByFid.forEach((fid, endringer) -> {
            var navident = getNavidentFromOrgtilknytningStateRepository(fid);
            if (navident != null) {
                otEndringerByNavident.putIfAbsent(navident, new ArrayList<>());
                otEndringerByNavident.get(navident).addAll(endringer);
            } else {
                log.warn("Failed to get navident from OrgtilknytningStateRepository for fid: {}", fid);
                removeFromOrgtilknytningEndringerQueue(fid);
            }
        });

        otEndringerByNavident.forEach((navident, endringer) -> {
            var endretRessurs = ressursStateRepository.findByNavident(navident);
            if (endretRessurs.isEmpty()) {
                log.error("Fant ikke ressurs med navident {} og epost varsel vil ikke bli sendt.", navident);
                // not cleared from the queue! will be retried next time
            } else {
                // check if the navident is mastered in nom
                if(nomMastered.isMastered(navident)) {
                    sendOTEndretEmail(endretRessurs.get(), endringer);
                } else {
                    log.info("Navident {} is not mastered in nom. OT changes email will not be sent!", navident);
                }

                // remove from the queue in any case
                otEndringerByFid.forEach((fid, otEndringer) -> {
                    if(navident.equals(getNavidentFromOrgtilknytningStateRepository(fid))) {
                        removeFromOrgtilknytningEndringerQueue(fid);
                    }
                });
            }
        });
    }

    private void sendOTEndretEmail(RessursState endretRessurs, List<OTEndretData> otEndringerData) {
        try {
            Set<String> recipientNavidents = getRecipientNavidents(otEndringerData);
            log.info("Sender OT endringer epost til navidents: {}", recipientNavidents);

            var adRessurser = adClient.getAdRessurser(recipientNavidents);
            var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();

            String subject = String.format("Endringer i org.tilknytninger for %s (%s)",
                endretRessurs.visningsnavn().getFormattedVisningsnavn(), endretRessurs.navident()
            );

            String otEndringer = composeOTChangesMessage(otEndringerData);

            String body = getFormattetEmailBody(
                endretRessurs.visningsnavn().getFormattedVisningsnavn(),
                endretRessurs.navident(),
                otEndringer
            );

            if (body.isBlank()) {
                log.warn("Epost body er tom, OR endringer epost varsel vil ikke bli sendt");
                return;
            }

            log.info("Sender OT endringer epost subject: {}, recipients: {}, message: {}", subject, recipientList, body);
            EmailMessage message = new EmailMessage(subject, body, recipientList, List.of(), getAdditionalBccRecipientsEmails());
            if (!unleash.isEnabled(UNLEASH_SKRU_AV_EPOST_ENDRING)) {
                microsoftGraphClient.sendEmail(message).subscribe();
            } else {
                log.info("Epost sending is disabled by Unleash");
            }
        } catch (ADResponseException e) {
            throw new RessursOpprettetEmailException("Fant ikke leder epost adresse for leder");
        } catch (IOException | TemplateException e) {
            throw new RessursOpprettetEmailException("Feil ved generering av epost innhold");
        }
    }

    public void sendMasseflyttOTEmail(
        String nyOrgenhetId,
        Collection<OrgtilknytningState> orgtilknytningerFlyttet,
        Collection<OrgtilknytningState> orgtilknytningerIkkeFlyttet
    ) {
        try {
            List<String> recipientList = new ArrayList<>();
            var innsenderVisningsnavn = "";

            recipientList.addAll(getAdditionalRecipientsEmails());
            if (SecurityUtil.hasAuthentication()) {
                String innsenderNavident = SecurityUtil.getAuthenticatedNavIdent();
                innsenderVisningsnavn = ressursStateRepository.findByNavident(innsenderNavident)
                    .map(RessursState::visningsnavn)
                    .map(Visningsnavn::getFormattedVisningsnavn)
                    .orElse("UKJENT");

                recipientList.addAll(adClient.getAdRessurser(Set.of(innsenderNavident)).getRessurser().stream()
                    .map(AdRessurs::getEmail)
                    .toList()
                );
            } else {
                // Auto flytting av OTer utenfor hierarki
                recipientList.addAll(getOmadaRecipientsEmails());
            }

            var gammelOrgEnhetNavn = orgtilknytningerFlyttet.stream().map(ot -> getOrgEnhetNavn(ot.orgEnhetId())).distinct().collect(Collectors.joining(", "));
            var nyOrgEnhetNavn = getOrgEnhetNavn(nyOrgenhetId);

            String subject = String.format("Masseflytting av ressurser fra %s til %s", gammelOrgEnhetNavn, nyOrgEnhetNavn);
            String body = getFormattedEmailBody(
                nyOrgEnhetNavn,
                gammelOrgEnhetNavn,
                orgtilknytningerFlyttet.size(),
                orgtilknytningerIkkeFlyttet.size(),
                orgtilknytningerFlyttet.stream().map(OrgtilknytningState::navident).toList(),
                orgtilknytningerIkkeFlyttet.stream().map(OrgtilknytningState::navident).toList(),
                innsenderVisningsnavn
            );
            log.info("Masseflytting epost. Mottakere: {}\nBody:\n{}", recipientList, body);
            EmailMessage message = new EmailMessage(subject, body, recipientList);
            microsoftGraphClient.sendEmail(message).subscribe();
        } catch (ADResponseException e) {
            log.error("Fant ikke innsender sin epost adresse for masseflytting", e);
        } catch (IOException | TemplateException e) {
            log.error("Feil ved generering av epost innhold", e);
        }
    }

    private String getOrgEnhetNavn(String orgenhetId) {
        var orgenhet = orgenhetService.getById(orgenhetId);

        if (orgenhet.isPresent()) {
            var oe = orgenhet.get();
            var remedyEnhet = remedyEnhetMapperService.findByNomId(oe.getId());
            return oe.getNavn() + " (" + oe.getId() + remedyEnhet.map(enhet -> "/" + enhet.remedyEnhetId()).orElse("") + ")";
        } else {
            return "Ukjent";
        }
    }

    private String getFormattedEmailBody(
        String nyOrgenhet,
        String gammelOrgEnheter,
        int antallFlyttet,
        int feiletAntall,
        List<String> flyttedeNavidenter,
        List<String> feiledeNavidenter,
        String innsenderVisningsnavn
    ) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "nyOrgEnhet", nyOrgenhet,
            "antallFlyttet", antallFlyttet,
            "feiletAntall", feiletAntall,
            "total", antallFlyttet + feiletAntall,
            "gammelOrgEnhet", gammelOrgEnheter,
            "flyttedeNavidenter", String.join(",\n", flyttedeNavidenter),
            "feiletNavidenter", String.join(",\n", feiledeNavidenter),
            "innsenderVisningsnavn", innsenderVisningsnavn
        );
        freemarkerConfig.getTemplate(MASSEFLYTTING_VARSEL_FTLH).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    private String getFormattetEmailBody(String resursName, String navIdent, String otEndringer) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "resursName", resursName,
            "navIdent", navIdent,
            "otEndringer", otEndringer
        );
        freemarkerConfig.getTemplate(OT_ENDRINGER_VARSEL_TEMPLATE).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    public String composeOTChangesMessage(List<OTEndretData> otEndringerData) {
        StringBuilder otEndringer = new StringBuilder();
        for (OTEndretData event : otEndringerData) {
            var orgtilknytningState = orgtilknytningStateRepository.findByFid(event.fid());
            var utfortAvNavIdent = event.changedByNavIdent();
            var utfortAvRessurs = ressursStateRepository.findByNavident(utfortAvNavIdent);
            String utfortAvRessursNavn = utfortAvRessurs.map(ressursState -> ressursState.visningsnavn().getFormattedVisningsnavn()).orElse("Ukjent");
            if (orgtilknytningState.isPresent()) {
                var ot = orgtilknytningState.get();
                log.info(
                    "Sender OT endringer for navident:{} orgtilknytningState: {}, and formated dates: {}, {}",
                    event.navident(),
                    ot,
                    DateUtils.formatDateTime(ot.gyldigFom()),
                    DateUtils.formatDateTime(ot.gyldigTom().isPresent() ? ot.gyldigTom().get() : null)
                );
                log.info("Sender OT endringer utfortAv: {}, {}, {}", utfortAvNavIdent, utfortAvRessurs, utfortAvRessursNavn);
                var orgenhetNavn = orgEnhetStateRepository.findById(ot.orgEnhetId()).map(OrgEnhetState::navn).orElse("Ukjent");
                String endring = MessageFormat.format(epost_body_item_template,
                    orgenhetNavn,
                    ot.orgEnhetId(),
                    DateUtils.formatDateTime(ot.gyldigFom()),
                    DateUtils.formatDateTime(ot.gyldigTom().isPresent() ? ot.gyldigTom().get() : null),
                    utfortAvRessursNavn,
                    event.timestamp()
                );
                // update message if not blank and duplicate
                if(!endring.isBlank() && !otEndringer.toString().contains(endring)) {
                    otEndringer.append(endring);
                }
            } else {
                log.warn("Fant ikke orgtilknytning for OTEndretData: {} i orgtilknytningStateRepository. Sjekk!", event);
                String endring = MessageFormat.format(epost_body_item_template_ot_notfound,
                    event.eventType(), utfortAvRessursNavn, event.timestamp()
                );
                otEndringer.append(endring);
            }

            if (!otEndringer.isEmpty()) {
                otEndringer.append("\n");
            }
        }
        return otEndringer.toString();
    }

    public Set<String> getRecipientNavidents(List<OTEndretData> otEndringerData) {
        Set<String> recipientNavidents = new HashSet<>();
        for (OTEndretData event : otEndringerData) {
            // add the changedBy person
            recipientNavidents.add(event.changedByNavIdent());
            var orgtilknytningState = orgtilknytningStateRepository.findByFid(event.fid());
            if (orgtilknytningState.isPresent()) {
                var orgenhetState = orgEnhetStateRepository.findById(orgtilknytningState.get().orgEnhetId());
                if (orgenhetState.isPresent()) {
                    log.info("Orgenhet ved OT endriner: {}", orgenhetState.get());
                    var leader = lederResolver.leder(
                        // populate at least agressoId, orgNiv, and navn
                        OrgEnhet.builder()
                            .id(orgenhetState.get().id())
                            .navn(orgenhetState.get().navn())
                            .agressoId(orgenhetState.get().agressoId().orElse(null))
                            .orgNiv(orgenhetState.get().agressoOrgNivaa().orElse(null))
                            .build()
                    );
                    log.info("Orgenhet og leder ved OT endriner: {}, {}", orgenhetState.get(), leader);
                    if (!leader.isEmpty()) {
                        // add the leader of the orgenhet
                        recipientNavidents.add(leader.getFirst().getRessurs().getNavIdent());
                    }
                }
            }
        }
        return recipientNavidents;
    }

    private List<String> getAdditionalRecipientsEmails() {
        // add Line and Mugge for testing purposes. This will be deleted later when tests are confirmed
        List<String> additionalRecipients = new ArrayList<>();
        additionalRecipients.add("Line.Eeg-Larsen@nav.no");
        additionalRecipients.add("Mugunthan.Dharmadas@nav.no");
        additionalRecipients.add("Roy.Grini@nav.no");
        return additionalRecipients;
    }

    private List<String> getAdditionalBccRecipientsEmails() {
        return unleash.isEnabled("nom.email.bcc-ot-endret") ? List.of("hama.keli@nav.no") : List.of();
    }

    private List<String> getOmadaRecipientsEmails() {
        List<String> additionalRecipients = new ArrayList<>();
        additionalRecipients.add("Eivind.Staff@nav.no");
        additionalRecipients.add("Lise.Charlotte.Tviberg@nav.no");
        return additionalRecipients;
    }


    public String getNavidentFromOrgtilknytningStateRepository(String fid) {
        var orgtilknytningState = orgtilknytningStateRepository.findByFid(fid);
        return orgtilknytningState.map(OrgtilknytningState::navident).orElse(null);
    }
}
