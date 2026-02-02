package no.nav.nom.apps.api;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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

    @MockitoSpyBean
    final NomStateRessursListener nomStateRessursListener;

    @MockitoSpyBean
    final NomStateMasteredRessursListener nomStateMasteredRessursListener;

    @MockitoSpyBean
    final NomStateOrgEnhetListener nomStateOrgEnhetListener;

    private final KafkaTemplate<String, RessursState> ressursStateKafkaTemplate;
    private final KafkaTemplate<String, OrgEnhetState> orgEnhetStateKafkaTemplate;

    @Autowired
    public KafkaUtils(
        EmbeddedKafkaBroker embeddedKafkaBroker,
        NomStateOrgEnhetListener nomStateOrgEnhetListener,
        @Value("${app.kafka.ressurs.stateTopic}") String ressursStateTopic,
        @Value("${app.kafka.orgEnhet.stateTopic}") String orgEnhetStateTopic,
        Admin adminClient, NomStateRessursListener nomStateRessursListener,
        NomStateMasteredRessursListener nomStateMasteredRessursListener
    ) {
        this.embeddedKafkaBroker = embeddedKafkaBroker;
        this.nomStateRessursListener = nomStateRessursListener;

        this.ressursStateTopic = ressursStateTopic;
        this.adminClient = adminClient;
        this.nomStateMasteredRessursListener = nomStateMasteredRessursListener;
        this.ressursStateKafkaTemplate = ressursStateKafkaTemplate();

        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
        this.orgenhetStateTopic = orgEnhetStateTopic;
        this.orgEnhetStateKafkaTemplate = orgEnhetStateKafkaTemplate();
    }


    public synchronized void sendRessurs(String key, RessursState ressursState) {
        CountDownLatch latch = new CountDownLatch(1);
        nomStateRessursListener.register(new AbstractNomStateListener.Visitor<>() {
            @Override
            public String visitorName() {
                return "KafakUtilTestVisitor";
            }

            @Override
            public void visit(String key, RessursState stateObject) {
                latch.countDown();
            }
        });

        ressursStateKafkaTemplate.sendDefault(key, ressursState);
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Ressurs state sent on topic is never read");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void sendOrgenhet(String key, OrgEnhetState orgEnhetState) {
        CountDownLatch latch = new CountDownLatch(1);
        nomStateOrgEnhetListener.register(new AbstractNomStateListener.Visitor<>() {
            @Override
            public String visitorName() {
                return "KafakUtilTestVisitor";
            }

            @Override
            public void visit(String key, OrgEnhetState stateObject) {
                latch.countDown();
            }
        });

        this.orgEnhetStateKafkaTemplate.sendDefault(key, orgEnhetState);
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
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
            new Jobbtelefon("12345678" + suffix, TelefonType.NAV_TJENESTE_TELEFON),
            null,
            null,
            LocalDate.now(),
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null,
            null
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
            null,
            null,
            null,
            null,
            null,
            null,
            "0000000".substring(0, 7 - suffix.length()) + suffix,
            "ORGENHET",
            null
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
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        ProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
        return new KafkaTemplate<>(producerFactory);
    }
}
