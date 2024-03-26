package no.nav.nom.apps.api;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateListener;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.ressurs.masterdata.NomMasteredRessursListener;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class KafkaUtils {

    private final String ressursStateTopic;

    private final String orgenhetStateTopic;

    private final EmbeddedKafkaBroker embeddedKafkaBroker;

    private final Admin adminClient;

    @SpyBean
    final RessursStateListener ressursStateListener;

    @SpyBean
    public final NomMasteredRessursListener nomMasteredRessursListener;

    @SpyBean
    final OrgEnhetStateListener orgEnhetStateListener;

    private final KafkaTemplate<String, RessursState> ressursStateKafkaTemplate;
    private final KafkaTemplate<String, OrgEnhetState> orgEnhetStateKafkaTemplate;

    @Autowired
    public KafkaUtils(
        EmbeddedKafkaBroker embeddedKafkaBroker,
        RessursStateListener ressursStateListener,
        OrgEnhetStateListener orgEnhetStateListener,
        @Value("${app.kafka.ressurs.stateTopic}") String ressursStateTopic,
        @Value("${app.kafka.orgEnhet.stateTopic}") String orgEnhetStateTopic,
        Admin adminClient,
        NomMasteredRessursListener nomMasteredRessursListener
    ) {
        this.embeddedKafkaBroker = embeddedKafkaBroker;

        this.ressursStateListener = ressursStateListener;
        this.ressursStateTopic = ressursStateTopic;
        this.adminClient = adminClient;
        this.nomMasteredRessursListener = nomMasteredRessursListener;
        this.ressursStateKafkaTemplate = ressursStateKafkaTemplate();

        this.orgEnhetStateListener = orgEnhetStateListener;
        this.orgenhetStateTopic = orgEnhetStateTopic;
        this.orgEnhetStateKafkaTemplate = orgEnhetStateKafkaTemplate();
    }


    public synchronized void sendRessurs(String key, RessursState ressursState) {
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(ressursStateListener).onMessage(Mockito.any(), Mockito.any());


        ressursStateKafkaTemplate.sendDefault(key, ressursState);
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation of kafka onMessage never occoured or never exited");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void sendOrgenhet(String key, OrgEnhetState orgEnhetState) {
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(this.orgEnhetStateListener).onMessage(Mockito.any(), Mockito.any());


        this.orgEnhetStateKafkaTemplate.sendDefault(key, orgEnhetState);
        try {
            if (!latch.await(900, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation of kafka onMessage never occoured or never exited");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public RessursState createRessursHendelseEventSource(String suffix) {
        final var ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "testPersonIdent" + suffix,
            "testNavIdent" + suffix,
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn" + suffix,
                Optional.of("mellomnavn" + suffix),
                "etternavn" + suffix
            ),
            null,
            Optional.of(LocalDate.now()),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursState);

        return ressursState;
    }


    public OrgEnhetState createOrgenhetHendelseEventSource(String suffix) {
        final var orgEnhetState = new OrgEnhetState(
            PrefixedUlidCreator.createOrgEnhetUlid(),
            "testOrgenhetId" + suffix,
            "testOrgenhetNavn" + suffix,
            LocalDate.now(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("0000000".substring(0, 7 - suffix.length()) + suffix),
            Optional.of("ORGENHET"),
            Optional.empty()
        );

        createOrgenhetHendelseEventSource(orgEnhetState);

        return orgEnhetState;
    }

    public void createRessursHendelseEventSource(RessursState ressursState) {
        sendRessurs(ressursState.fid(), ressursState);
    }

    public void createOrgenhetHendelseEventSource(OrgEnhetState orgEnhetState) {
        sendOrgenhet(orgEnhetState.fid(), orgEnhetState);
    }

    public void clearKafka() {
        try {
            Map<TopicPartition, RecordsToDelete> collect = adminClient.listPartitionReassignments()
                .reassignments()
                .get(10, TimeUnit.SECONDS)
                .keySet().stream()
                .collect(Collectors.toMap(Function.identity(), topicPartition -> RecordsToDelete.beforeOffset(Long.MAX_VALUE)));

            adminClient.deleteRecords(collect).all().get();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to clear topic", exception);
        }
    }

    private KafkaTemplate<String, RessursState> ressursStateKafkaTemplate() {
        var out = this.<RessursState>defaultKafkaTemplate();
        out.setDefaultTopic(ressursStateTopic);
        return out;
    }

    private KafkaTemplate<String, OrgEnhetState> orgEnhetStateKafkaTemplate() {
        var out = this.<OrgEnhetState>defaultKafkaTemplate();
        out.setDefaultTopic(orgenhetStateTopic);
        return out;
    }

    @NotNull
    private <T> KafkaTemplate<String, T> defaultKafkaTemplate() {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
        return new KafkaTemplate<>(producerFactory);
    }
}
