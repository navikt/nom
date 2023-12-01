package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RessursMetadataReposotoryImpl implements RessursMetadataRepository {

    private Map<String, Event.Metadata> metadataById= new ConcurrentHashMap<>();

    @Override
    public void save(String id, Event.Metadata metadata) {
        metadataById.put(id, metadata);
    }

    @Override
    public String findOpprettetAvById(String id) {
        var metadata = metadataById.get(id);

        return metadata != null
            ? metadata.inputMetadata().origination().initiatedBy()
            : null;
    }

    @Override
    public LocalDateTime findOpprettetDatoById(String id) {
        var metadata = metadataById.get(id);

        return metadata != null
            ? metadata.createdTimestamp()
            : null;
    }

    @Override
    public int count() {
        return metadataById.size();
    }
}
