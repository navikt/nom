package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
public class EvtsrcKafkaListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(EvtsrcKafkaListener.class);

    static final String ORGENHET_LISTENER_ID       = "evtsrc-orgenhet-listener";
    static final String ORGENHET_LEDER_LISTENER_ID = "evtsrc-orgenhet-leder-listener";
    static final String ORGANISERING_LISTENER_ID   = "evtsrc-organisering-listener";

    private final OrganisasjonHendelseRepository repository;
    private final JsonMapper objectMapper;
    private final EvtsrcKafkaProperties evtsrcProperties;
    private final AdminClient evtsrcAdminClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final KafkaListenerEndpointRegistry listenerRegistry;

    public EvtsrcKafkaListener(
        OrganisasjonHendelseRepository repository,
        JsonMapper objectMapper,
        EvtsrcKafkaProperties evtsrcProperties,
        AdminClient evtsrcAdminClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        OrganiseringStateRepository organiseringStateRepository,
        KafkaListenerEndpointRegistry listenerRegistry
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.evtsrcProperties = evtsrcProperties;
        this.evtsrcAdminClient = evtsrcAdminClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.listenerRegistry = listenerRegistry;
    }

    @KafkaListener(
        id = ORGENHET_LISTENER_ID,
        topics = "${app.kafka.evtsrc.orgenhet.topic}",
        autoStartup = "${app.kafka.evtsrc.auto-startup}",
        containerFactory = "evtsrcKafkaListenerContainerFactory"
    )
    public void orgenhetListener(String melding, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        prosesserEvtsrcMelding(melding, key, this::hentOrgEnhetIdFraOrgenhetState, "orgenhet");
    }

    @KafkaListener(
        id = ORGENHET_LEDER_LISTENER_ID,
        topics = "${app.kafka.evtsrc.orgenhet-leder.topic}",
        autoStartup = "${app.kafka.evtsrc.auto-startup}",
        containerFactory = "evtsrcKafkaListenerContainerFactory"
    )
    public void orgenhetLederListener(String melding, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        prosesserEvtsrcMelding(melding, key, this::hentOrgEnhetIdFraOrgenhetLederState, "orgenhet-leder");
    }

    @KafkaListener(
        id = ORGANISERING_LISTENER_ID,
        topics = "${app.kafka.evtsrc.organisering.topic}",
        autoStartup = "${app.kafka.evtsrc.auto-startup}",
        containerFactory = "evtsrcKafkaListenerContainerFactory"
    )
    public void organiseringListener(String melding, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        prosesserEvtsrcMelding(melding, key, this::hentOrgEnhetIdFraOrganiseringState, "organisering");
    }

    /**
     * Felles prosesseringsflyt for alle tre evtsrc-topics:
     * parser fid, slår opp orgEnhetId via NOM state, og lagrer hendelsen.
     * Håndterer race condition ved at state opprettes asynkront som følge av evtsrc.
     */
    private void prosesserEvtsrcMelding(String melding, String key,
                                        BiFunction<String, String, String> orgEnhetIdLookup,
                                        String topicNavn) {
        try {
            String fid = parseFid(melding);
            if (fid == null || fid.isBlank()) {
                throw new IllegalArgumentException("Melding mangler påkrevde felter (fid, type)");
            }
            lagreMelding(melding, key, orgEnhetIdLookup.apply(fid, key));
        } catch (OrgEnhetStateIkkeFunnetException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Feil ved prosessering av {} evtsrc-melding. event_key={}", topicNavn, key, e);
            throw new RuntimeException("Feil ved prosessering av " + topicNavn + " evtsrc-melding", e);
        }
    }

    // ── NOM state-oppslag med retry ───────────────────────────────────────────

    private String hentOrgEnhetIdFraOrgenhetState(String fid, String eventKey) {
        return hentIdMedRetryOgStopp(fid, eventKey,
            f -> orgEnhetStateRepository.findByFid(f).map(OrgEnhetState::id),
            ORGENHET_LISTENER_ID);
    }

    private String hentOrgEnhetIdFraOrgenhetLederState(String fid, String eventKey) {
        return hentIdMedRetryOgStopp(fid, eventKey,
            f -> orgEnhetLederStateRepository.findByFid(f).map(OrgEnhetLederState::orgEnhetId),
            ORGENHET_LEDER_LISTENER_ID);
    }

    private String hentOrgEnhetIdFraOrganiseringState(String fid, String eventKey) {
        return hentIdMedRetryOgStopp(fid, eventKey,
            f -> organiseringStateRepository.findByFid(f).map(OrganiseringState::id),
            ORGANISERING_LISTENER_ID);
    }

    /**
     * Slår opp ID fra NOM state via FID med én retry etter 2 sekunder.
     * Stopper den angitte listener-containeren og kaster exception ved vedvarende feiling
     * slik at offset ikke committes og meldingen re-prosesseres ved manuell restart.
     */
    private String hentIdMedRetryOgStopp(String fid, String eventKey,
                                          Function<String, Optional<String>> lookup,
                                          String listenerIdToStop) {
        var id = lookup.apply(fid);
        if (id.isPresent()) {
            return id.get();
        }

        log.warn("State ikke funnet for fid={} (event_key={}). Mulig race condition – venter {} ms og prøver igjen.", fid, eventKey, evtsrcProperties.getRetryDelayMs());
        try {
            Thread.sleep(evtsrcProperties.getRetryDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var idEtterVent = lookup.apply(fid);
        if (idEtterVent.isPresent()) {
            return idEtterVent.get();
        }

        log.error("ALERTING EVTSRC: State ikke funnet for fid={} (event_key={}) etter retry. Stopper listener [{}].",
            fid, eventKey, listenerIdToStop);
        stoppListener(listenerIdToStop);
        throw new OrgEnhetStateIkkeFunnetException("State ikke funnet for fid=" + fid + " etter retry");
    }

    private void stoppListener(String listenerId) {
        CompletableFuture.runAsync(() -> {
            MessageListenerContainer container = listenerRegistry.getListenerContainer(listenerId);
            if (container != null) {
                log.warn("Stopper evtsrc listener container [{}]", listenerId);
                container.stop();
            }
        });
    }

    // ── Lagring ───────────────────────────────────────────────────────────────

    private void lagreMelding(String melding, String eventKey, String orgEnhetId) {
        try {
            JsonNode root = objectMapper.readTree(melding);
            String fid = root.path("fid").asText(null);
            String eventType = root.path("type").asText(null);

            if (fid == null || fid.isBlank() || eventType == null || eventType.isBlank()) {
                log.error("Ugyldig eventsource-melding – mangler fid eller type. event_key={}", eventKey);
                throw new IllegalArgumentException("Melding mangler påkrevde felter (fid, type)");
            }

            repository.save(OrganisasjonHendelse.builder()
                .mottattTidspunkt(OffsetDateTime.now())
                .fid(fid)
                .eventKey(eventKey)
                .orgEnhetId(orgEnhetId)
                .eventType(eventType)
                .melding(melding)
                .build());

            log.info("Lagret eventsource-hendelse: type={}, fid={}, orgEnhetId={}", eventType, fid, orgEnhetId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Feil ved lagring av eventsource-melding. event_key={}", eventKey, e);
            throw new RuntimeException("Feil ved lagring av eventsource-melding", e);
        }
    }

    private String parseFid(String melding) throws Exception {
        return objectMapper.readTree(melding).path("fid").asText(null);
    }

    // ── ConsumerSeekAware ─────────────────────────────────────────────────────

    /**
     * Kalles ved tilordning av partisjoner. Ved første oppstart (ingen committed offsets) søker vi til
     * det per-topic konfigurerte startoffset. Ved restart brukes committed offsets som normalt.
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        try {
            var committedOffsets = evtsrcAdminClient
                .listConsumerGroupOffsets(evtsrcProperties.getGroupId())
                .partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS);

            Map<String, Long> startOffsetPerTopic = Map.of(
                evtsrcProperties.getOrgenhet().getTopic(),      evtsrcProperties.getOrgenhet().getStartOffset(),
                evtsrcProperties.getOrgenhetLeder().getTopic(), evtsrcProperties.getOrgenhetLeder().getStartOffset(),
                evtsrcProperties.getOrganisering().getTopic(),  evtsrcProperties.getOrganisering().getStartOffset()
            );

            assignments.keySet().forEach(tp -> {
                if (!committedOffsets.containsKey(tp)) {
                    long startOffset = startOffsetPerTopic.getOrDefault(tp.topic(), 0L);
                    if (startOffset < 0) {
                        log.info("Ingen committed offset for {} – start-offset=-1, bruker auto.offset.reset (latest)", tp);
                    } else {
                        log.info("Ingen committed offset for {} – søker til konfigurert startoffset {}", tp, startOffset);
                        callback.seek(tp.topic(), tp.partition(), startOffset);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Kunne ikke sjekke committed offsets, bruker auto.offset.reset", e);
        }
    }
}
