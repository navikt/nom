package no.nav.nom.apps.streams.kafkaapi.hendelser;

import no.nav.nom.apps.streams.kafkaapi.hendelser.v1.Hendelse;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelseType;
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
public class EventsrcToHendelser {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToHendelser.class);
    public static final String NOM_HENDELSER_STATE_STORE = "nom-hendelser-state-store";

    public EventsrcToHendelser(
        @Value("${app.kafka.orgenhet-evtsrc-topic}") String orgenhetEvtsrcTopic,
        @Value("${app.kafka.ressurs-evtsrc-topic}") String ressursEvtsrcTopic,
        @Value("${app.kafka.organisering-evtsrc-topic}") String organiseringEvtsrcTopic,
        @Value("${app.kafka.orgtilknytning-evtsrc-topic}") String orgtilknytningEvtsrcTopic,
        @Value("${app.kafka.state-topic.v1}") String stateTopic,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var hendelserSerde = objectSerde.copyWithType(Hendelse.class);
        final var eventSerde = objectSerde.copyWithType(Event.class);

        KStream<String, Hendelse> hendelserStream = streamsBuilder
            .addStateStore(
                Stores.keyValueStoreBuilder(
                    Stores.persistentKeyValueStore(NOM_HENDELSER_STATE_STORE),
                    Serdes.String(),
                    Serdes.String()
                )
            )
            .stream(
                Arrays.asList(orgenhetEvtsrcTopic, organiseringEvtsrcTopic, ressursEvtsrcTopic, orgtilknytningEvtsrcTopic),
                Consumed.with(stringSerde, eventSerde)
            )
            .map((k, v) -> KeyValue.pair(v.getFid(), toHendelser(v)))
            .process(new ProcessorSupplier<>() {
                @Override
                public Set<StoreBuilder<?>> stores() {
                    return ProcessorSupplier.super.stores();
                }

                @Override
                public Processor<String, Hendelse, String, Hendelse> get() {
                    return new Processor<>() {
                        private KeyValueStore<String, String> hendelserStore;
                        private ProcessorContext<String, Hendelse> context;

                        @Override
                        public void init(ProcessorContext<String, Hendelse> context) {
                            Processor.super.init(context);
                            this.hendelserStore = context.getStateStore(NOM_HENDELSER_STATE_STORE);
                            this.context = context;
                        }

                        @Override
                        public void close() {
                            Processor.super.close();
                        }

                        @Override
                        public void process(Record<String, Hendelse> record) {
                            Hendelse incomingHendelse = record.value();
                            if (incomingHendelse.timestamp() == null) {
                                log.warn("Incoming hendelser timestamp is null for " + record.key());
                            }

                            String hendelseIdInStoreForGivenFid = hendelserStore.get(record.key());
                            if (hendelseIdInStoreForGivenFid != null && (incomingHendelse.id() == null || incomingHendelse.id().isBlank())) {
                                incomingHendelse = incomingHendelse.toBuilder().id(hendelseIdInStoreForGivenFid).build();
                            }

                            hendelserStore.put(record.key(), incomingHendelse.id());

                            // do not the forward if hendelserType is unknown
                            if(incomingHendelse.hendelseType() != null){
                                context.forward(new Record<>(record.key(), incomingHendelse, record.timestamp()));
                            }
                        }
                    };
                }
            }, NOM_HENDELSER_STATE_STORE);

        hendelserStream.to(stateTopic, Produced.with(stringSerde, hendelserSerde));
    }

    private Hendelse toHendelser(Event event) {
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

    private Hendelse convert(OrgEnhetEvent event) {
        try {
            var hendelserStateBuilder = Hendelse.builder()
                .entityType(EntityType.ORGENHET)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getData() instanceof OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelseType(HendelseType.NY);
            } else if (event.getData() instanceof OrgEnhetKorrigertEventV1.OrgEnhetKorrigertEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData) {
                hendelserStateBuilder
                    .hendelseType(HendelseType.FJERNET);
            } else {
                hendelserStateBuilder
                    .hendelseType(HendelseType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from OrgEnhetEvent. ", e);
            throw e;
        }
    }

    private Hendelse convert(RessursEvent event) {
        try {
            var hendelserStateBuilder = Hendelse.builder()
                .entityType(EntityType.RESSURS)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data) {
                hendelserStateBuilder
                    .id(data.navident().orElse(null))
                    .hendelseType(HendelseType.NY);
            } else if (event.getData() instanceof RessursSluttetEventV1.RessursSluttetEventData) {
                hendelserStateBuilder
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof RessursTekniskSlettetEventV1.RessursTekniskSlettetEventData) {
                hendelserStateBuilder
                    .hendelseType(HendelseType.FJERNET);
            }else {
                hendelserStateBuilder
                    .hendelseType(HendelseType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from RessursEvent. ", e);
            throw e;
        }
    }

    private Hendelse convert(OrganiseringEvent event) {
        try {
            var hendelserStateBuilder = Hendelse.builder()
                .entityType(EntityType.ORGENHET)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getData() instanceof OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof OrganiseringEndretEventV1.OrganiseringEndretEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelseType(HendelseType.ENDRET);
            } else {
                hendelserStateBuilder
                    .hendelseType(HendelseType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from OrganniseringEvent. ", e);
            throw e;
        }
    }

    private Hendelse convert(OrgtilknytningEvent event) {
        try {
            var hendelserStateBuilder = Hendelse.builder()
                .entityType(EntityType.RESSURS)
                .timestamp(event.getMetadata() != null ? event.getMetadata().createdTimestamp() : null);

            if (event.getData() instanceof OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningFjernetEventV1.OrgtilknytningFjernetEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningDatoEndretEventV1.OrgtilknytningDatoEndretEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelseType(HendelseType.ENDRET);
            } else if (event.getData() instanceof OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData data) {
                hendelserStateBuilder
                    .id(data.navident())
                    .hendelseType(HendelseType.ENDRET);
            } else {
                hendelserStateBuilder
                    .hendelseType(HendelseType.ENDRET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error converting to Hendelser from OrgtilknytningEvent. ", e);
            throw e;
        }
    }

}
