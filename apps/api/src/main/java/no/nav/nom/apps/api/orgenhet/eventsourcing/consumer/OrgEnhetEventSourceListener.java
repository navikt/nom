package no.nav.nom.apps.api.orgenhet.eventsourcing.consumer;

import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.OrgEnhetNomNivaaHandler;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.stream.StreamSupport;

@Service
public class OrgEnhetEventSourceListener {
    private final OrgEnhetTypeHandler orgEnhetTypeHandler;
    private final OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler;
    private final Logger logger = LoggerFactory.getLogger(OrgEnhetEventSourceListener.class);
    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;

    public OrgEnhetEventSourceListener(
        OrgEnhetTypeHandler orgEnhetTypeHandler,
        OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler,
        DvhStartupLoadMonitor dvhStartupLoadMonitor
    ) {
        this.orgEnhetTypeHandler = orgEnhetTypeHandler;
        this.orgEnhetNomNivaaHandler = orgEnhetNomNivaaHandler;
        this.dvhStartupLoadMonitor = dvhStartupLoadMonitor;
    }

    @KafkaListener(
        id = "nomApi_TypeAndNivaaHandler",
        containerFactory = "orgEnhetEventSourceKafkaListenerContainerFactory",
        clientIdPrefix = "orgEnhetEventSourceListener",
        topics = "${app.kafka.orgEnhet.eventsourceTopic}"
    )
    public void onMessage(ConsumerRecords<String, OrgEnhetEvent> records, Consumer<String, String> consumer) {

        dvhStartupLoadMonitor.awaitInitialLoadComplete();

        StreamSupport.stream(records.spliterator(), false).forEach(consumerRecord -> {
            var orgEnhetEvent = consumerRecord.value();
            if (orgEnhetEvent instanceof OrgEnhetTypeEndretEventV1 orgEnhetEndretEvent) {
                orgEnhetTypeHandler.orgEnhetsTypeChanged(orgEnhetEndretEvent);
                orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(orgEnhetEndretEvent);

                logger.info(
                    "Listener received message with fid {} and orgEnhetsType {}", orgEnhetEvent.getFid(),
                    orgEnhetEndretEvent.getData().orgEnhetsType());
            }
        });
    }
}
