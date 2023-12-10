package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.Hendelser;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;
import no.nav.nom.contract.nom.eventsource.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;


@Component
public class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);
    public static final String NOM_HENDELSER_STATE_STORE = "nom-hendelser-state-store";
    public static final String NOM_HENDELSER_STATE_STORE_1 = "nom-hendelser-state-store_1";

    public EventsrcToState(
        @Value("${app.kafka.orgenhet-evtsrc-topic}") String orgenhetEvtsrcTopic,
        @Value("${app.kafka.ressurs-evtsrc-topic}") String ressursEvtsrcTopic,
        @Value("${app.kafka.organisering-evtsrc-topic}") String organiseringEvtsrcTopic,
        @Value("${app.kafka.orgtilknytning-evtsrc-topic}") String orgtilknytningEvtsrcTopic,
        @Value("${app.kafka.state-topic.v1}") String stateTopic,
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var jsonSerde = objectSerde.copyWithType(ObjectNode.class);
        final var orgenhetEventSerde = objectSerde.copyWithType(OrgEnhetEvent.class);
        final var ressursEventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var hendelserSerde = objectSerde.copyWithType(Hendelser.class);
        final var organiseringEventSerde = objectSerde.copyWithType(OrganiseringEvent.class);
        final var orgtilknytningEventSerde = objectSerde.copyWithType(OrgtilknytningEvent.class);
        final var eventSerde = objectSerde.copyWithType(Event.class);

        KStream<String, Hendelser> hendelserStream = streamsBuilder
            .addStateStore(
                Stores.keyValueStoreBuilder(
                    Stores.persistentKeyValueStore(NOM_HENDELSER_STATE_STORE),
                    Serdes.String(),
                    hendelserSerde
                )
            )
            .stream(Arrays.asList(orgenhetEvtsrcTopic, organiseringEvtsrcTopic, ressursEvtsrcTopic), Consumed.with(stringSerde, eventSerde))
            .map((k, v) -> KeyValue.pair(v.getFid(), toHendelser(v)))
            .process(new ProcessorSupplier<String, Hendelser, String, Hendelser>() {
                @Override
                public Set<StoreBuilder<?>> stores() {
                    return ProcessorSupplier.super.stores();
                }

                @Override
                public Processor<String, Hendelser, String, Hendelser> get() {
                    return new Processor<String, Hendelser, String, Hendelser>() {
                        private KeyValueStore<String, Hendelser> hendelserStore;
                        private ProcessorContext<String, Hendelser> context;

                        @Override
                        public void init(ProcessorContext<String, Hendelser> context) {
                            Processor.super.init(context);
                            this.hendelserStore = context.getStateStore(NOM_HENDELSER_STATE_STORE);
                            this.context = context;
                        }

                        @Override
                        public void close() {
                            Processor.super.close();
                        }

                        @Override
                        public void process(Record<String, Hendelser> record) {
                            Hendelser incomingHendelser = record.value();
                            Hendelser hendelserInStoreForGivenFid = hendelserStore.get(record.key());

                            if (hendelserInStoreForGivenFid != null && hendelserInStoreForGivenFid.id() != null &&
                                (incomingHendelser.id() == null || incomingHendelser.id().isBlank())) {
                                incomingHendelser = incomingHendelser.toBuilder().id(hendelserInStoreForGivenFid.id()).build();
                            }

                            hendelserStore.put(record.key(), incomingHendelser);
                            context.forward(new Record<>(record.key(), incomingHendelser, record.timestamp()));
                        }
                    };
                }
            }, new String[]{NOM_HENDELSER_STATE_STORE});

        hendelserStream.to(stateTopic, Produced.with(stringSerde, hendelserSerde));
    }

    private Hendelser toHendelser(Event event) {
        try {
            return
                event instanceof OrgEnhetEvent ? convert((OrgEnhetEvent) event) :
                event instanceof RessursEvent ? convert((RessursEvent) event) :
                event instanceof OrganiseringEvent ? convert((OrganiseringEvent) event) :
                event instanceof OrgtilknytningEvent ? convert((OrgtilknytningEvent) event) : null;
        } catch (Exception e) {
            log.error("Error mapping to Hendelser. ", e);
            throw e;
        }
    }

    private Hendelser convert(OrgEnhetEvent event) {
        try {
            var hendelserStateBuilder = Hendelser.builder()
                .entityType(EntityType.ORGENHET)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getData() instanceof OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelserType(HendelserType.NY);
            } else if (event.getData() instanceof OrgEnhetKorrigertEventV1.OrgEnhetKorrigertEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData) {
                hendelserStateBuilder
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData) {
                hendelserStateBuilder
                    .hendelserType(HendelserType.FJERNET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from OrgEnhetEvent. ", e);
            throw e;
        }
    }

    private Hendelser convert(RessursEvent event) {
        try {
            var hendelserStateBuilder = Hendelser.builder()
                .entityType(EntityType.RESSURS)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data) {
                hendelserStateBuilder
                    .id(data.navident().orElse(null))
                    .hendelserType(HendelserType.NY);
            } else if (event.getData() instanceof RessursSluttetEventV1.RessursSluttetEventData) {
                hendelserStateBuilder
                    .hendelserType(HendelserType.FJERNET);
            } else {
                hendelserStateBuilder
                    .hendelserType(HendelserType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from RessursEvent. ", e);
            throw e;
        }
    }

    private Hendelser convert(OrganiseringEvent event) {
        try {
            var hendelserStateBuilder = Hendelser.builder()
                .entityType(EntityType.ORGENHET)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getData() instanceof OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrganiseringEndretEventV1.OrganiseringEndretEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelserType(HendelserType.ENDRET);
            } else {
                hendelserStateBuilder
                    .hendelserType(HendelserType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from OrganniseringEvent. ", e);
            throw e;
        }
    }

    private Hendelser convert(OrgtilknytningEvent event) {
        try {
            var hendelserStateBuilder = Hendelser.builder()
                .entityType(EntityType.RESSURS)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getData() instanceof OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningFjernetEventV1.OrgtilknytningFjernetEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningDatoEndretEventV1.OrgtilknytningDatoEndretEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelserType(HendelserType.ENDRET);
            }
            else {
                hendelserStateBuilder
                    .hendelserType(HendelserType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from OrgtilknytningEvent. ", e);
            throw e;
        }
    }

}
