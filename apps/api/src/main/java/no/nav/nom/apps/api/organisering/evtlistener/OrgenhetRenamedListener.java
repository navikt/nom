package no.nav.nom.apps.api.organisering.evtlistener;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.ad.ADResponseException;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrgenhetRenamedListener {
    public static final String ORGENHET_NAVN_ENDRET_VARSEL_FTLH = "orgenhet-navn-endret-varsel.ftlh";

    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final Configuration freemarkerConfig;
    private final MicrosoftGraphClient microsoftGraphClient;
    private final AppProperties.EpostVarselForEnhetNavnendring params;
    private final ObjectMapper objectMapper;

    public OrgenhetRenamedListener(
        DvhStartupLoadMonitor dvhStartupLoadMonitor, OrgenhetService orgenhetService,
        RemedyEnhetMapperService remedyEnhetMapperService, Configuration freemarkerConfig, MicrosoftGraphClient microsoftGraphClient,
        AppProperties appProperties, ObjectMapper objectMapper
    ) {
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.freemarkerConfig = freemarkerConfig;
        this.microsoftGraphClient = microsoftGraphClient;
        this.params = appProperties.epostVarselForEnhetNavnendring();
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        id = "nomApi_omada_orgenhet_renamed_listener",
        containerFactory = "organiseringEventSourceKafkaListenerContainerFactory",
        topics = "${app.kafka.organisering.eventsourceTopic}"
    )
    public void onMessage(ConsumerRecords<String, OrganiseringEvent> records) {
        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        StreamSupport.stream(records.spliterator(), false).forEach(consumerRecord -> {
            var organiseringEvent = consumerRecord.value();
            if (organiseringEvent instanceof OrganiseringAvsluttetEventV1 avsluttet) {
                handleOrganiseringAvsluttet(avsluttet);
            }
        });
    }

    private void handleOrganiseringAvsluttet(OrganiseringAvsluttetEventV1 avsluttetEvt) {
        var fid = avsluttetEvt.getFid();
        var organisering = getAvsluttetOrganiseringState(avsluttetEvt);
        if (organisering != null) {
            var opprinneligEnhet = orgenhetService.getById(organisering.id()).orElse(null);
            if (opprinneligEnhet == null) {
                log.error("Fant ikke orgenhet for Organisering med id {} for avsluttet-organisering event med fid {}", organisering.id(), fid);
                return;
            }
            var alternativEnhet = finnAlternativOrgenhet(opprinneligEnhet);
            if (alternativEnhet == null) {
                log.error("Fant ikke alternativ orgenhet for orgenhet med id {} for avsluttet-organisering event med fid {}", opprinneligEnhet.getId(), fid);
                return;
            }
            if (opprinneligEnhet.getId().equals(alternativEnhet.getId())) {
                log.info("Opprinnelig Orgenhet med id {} er samme som alternativ enhet {}. Ingen navnendring for avsluttet-organisering event med fid {}", opprinneligEnhet.getId(), alternativEnhet.getId(), fid);
                return;
            }
            var opprinneligEnhetRemedyEnhet = remedyEnhetMapperService.findByNomId(opprinneligEnhet.getId());
            if (opprinneligEnhetRemedyEnhet.isEmpty()) {
                log.info("Fant ikke RemedyEnhet for orgenhet med id {} for avsluttet-organisering event med fid {}. Antar da at enhet ikke er 4-siffer enhet toppnode", opprinneligEnhet.getId(), fid);
                return;
            }
            var alternativEnhetRemedyEnhet = remedyEnhetMapperService.findByNomId(alternativEnhet.getId());
            if (alternativEnhetRemedyEnhet.isEmpty()) {
                log.error("Fant ikke RemedyEnhet for alternativ orgenhet med id {} for avsluttet-organisering event med fid {}", alternativEnhet.getId(), fid);
                return;
            }
            if (!opprinneligEnhetRemedyEnhet.get().remedyEnhetId().equals(alternativEnhetRemedyEnhet.get().remedyEnhetId())) {
                log.info("Remedy enhet id {} for enhet tilhørende avsluttet-organisering event er ikke lik remedy enhet id {} for alternativ aktiv enhet med samme agressoId og nivå", opprinneligEnhetRemedyEnhet.get().remedyEnhetId(), alternativEnhetRemedyEnhet.get().remedyEnhetId());
            }
            if (avsluttetEvt.getMetadata().createdTimestamp().isBefore(LocalDate.now().minusDays(params.maxEvtAlderDagerForVarsel()).atStartOfDay())) {
                log.info("Gammel mulig navnendring på enhet {} til enhet {} for avsluttet-organisering event med fid {}. Sender ikke mail", opprinneligEnhet.getId(), alternativEnhet.getId(), fid);
            } else {
                sendEpostVarsling(
                    opprinneligEnhet,
                    alternativEnhet,
                    opprinneligEnhetRemedyEnhet.get().remedyEnhetId(),
                    alternativEnhetRemedyEnhet.get().remedyEnhetId()
                );
            }
        }
    }

    private void sendEpostVarsling(IOrgenhet gammelEnhet, IOrgenhet nyEnhet, String gammelRemedyEnhetId, String nyRemedyEnhetId) {
        try {
            List<String> recipientList = new ArrayList<>();
            recipientList.addAll(getTeamOrgRecipientsEmails());
            recipientList.addAll(getOmadaRecipientsEmails());

            var gammel = getSammensattNavn(gammelEnhet.getId(), gammelRemedyEnhetId, gammelEnhet.getNavn());
            var ny = getSammensattNavn(nyEnhet.getId(), nyRemedyEnhetId, nyEnhet.getNavn());

            String subject = String.format("Navnendring orgenhet - %s til %s", gammel, ny);
            String body = getFormattedEmailBody(
                gammel,
                ny
            );
            log.info("Orgenhet endret navn epost. Mottakere: {}\nBody:\n{}", recipientList, body);
            EmailMessage message = new EmailMessage(subject, body, recipientList);
            microsoftGraphClient.sendEmail(message).subscribe();
        } catch (
        ADResponseException e) {
            log.error("Fant ikke innsender sin epost adresse for masseflytting", e);
        } catch (IOException | TemplateException e) {
            log.error("Feil ved generering av epost innhold", e);
        }
    }

    private String getSammensattNavn(String orgenhetId, String remedyEnhetId, String orgenhetNavn) {
        return orgenhetNavn + " (" + orgenhetId + "/" + remedyEnhetId + ")";
    }

    private OrganiseringState getAvsluttetOrganiseringState(OrganiseringAvsluttetEventV1 avsluttetEvt) {
        var gyldigTom = ((OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData)avsluttetEvt.getData()).gyldigTom();
        if (gyldigTom == null || gyldigTom.isAfter(LocalDate.now())) {
            log.error("Gyldig tom er null eller ikke passert. Kan ikke håndtere det i denne konteksten. Fid: {}, Gyldig tom: {}",
                avsluttetEvt.getFid(), gyldigTom);
            return null;
        }
        // Lage fake OrganiseringState objekt siden state repo ikke har historikk
        var input = objectMapper.valueToTree(avsluttetEvt.getMetadata().inputMetadata().inputInterfaceMethodArguments());
        var id = input.get("id").asText();
        var navn = input.get("navn").asText();
        var idOver = input.get("idOver").asText();
        var navnOver = input.get("navnOver").asText();
        var gyldigFom = LocalDate.parse(input.get("gyldigFom").asText(), DateTimeFormatter.ISO_DATE);
        var organiseringState = new OrganiseringState(
            avsluttetEvt.getFid(),
            id,
            navn,
            idOver,
            navnOver,
            gyldigFom,
            Optional.of(gyldigTom)
        );
        return organiseringState;
    }

    private IOrgenhet finnAlternativOrgenhet(IOrgenhet faktiskOrgenhet) {
        var aktiveOrgenheter = orgenhetService.getAllEnheterMedOrganisering();
        return aktiveOrgenheter.stream()
            .filter(oe -> oe.getAgressoId().equals(faktiskOrgenhet.getAgressoId()))
            .filter(oe -> oe.getOrgNiv().equals(faktiskOrgenhet.getOrgNiv()))
            .findFirst().orElse(null);
    }

    private String getFormattedEmailBody(
        String gammelOrgEnhet,
        String nyOrgenhet
    ) throws IOException, TemplateException {
        var stringWriter = new StringWriter();
        var model = Map.of(
            "gammelOrgEnhet", gammelOrgEnhet,
            "nyOrgEnhet", nyOrgenhet
        );
        freemarkerConfig.getTemplate(ORGENHET_NAVN_ENDRET_VARSEL_FTLH).process(model, stringWriter);
        return stringWriter.getBuffer().toString();
    }

    private List<String> getTeamOrgRecipientsEmails() {
        // add Line and Mugge for testing purposes. This will be deleted later when tests are confirmed
        List<String> additionalRecipients = new ArrayList<>();
        additionalRecipients.add("Line.Eeg-Larsen@nav.no");
        additionalRecipients.add("Mugunthan.Dharmadas@nav.no");
        additionalRecipients.add("Roy.Grini@nav.no");
        return additionalRecipients;
    }

    private List<String> getOmadaRecipientsEmails() {
        List<String> additionalRecipients = new ArrayList<>();
        additionalRecipients.add("Eivind.Staff@nav.no");
        additionalRecipients.add("Lise.Charlotte.Tviberg@nav.no");
        additionalRecipients.add("bjornar.aarnes@nav.no");
        additionalRecipients.add("stale.kristoffersen@nav.no");
        additionalRecipients.add("frode.kristiansen@nav.no");
        additionalRecipients.add("Sara.Lehne.Engesvik@nav.no");
        return additionalRecipients;
    }
}
