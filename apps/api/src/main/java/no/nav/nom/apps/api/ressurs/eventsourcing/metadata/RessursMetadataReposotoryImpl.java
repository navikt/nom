package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RessursMetadataReposotoryImpl implements RessursMetadataRepository {

    private Map<String, Event.Metadata> opprettetMetadataByNavident = new ConcurrentHashMap<>();
    private Map<String, MetadataWithType> endringMetadataByNavident = new ConcurrentHashMap<>();

    public record MetadataWithType (
        Event.Metadata metadata,
        String type
    ) {}

    @Override
    public void saveOpprettelseMetadata(String navident, Event.Metadata metadata) {
        opprettetMetadataByNavident.put(navident, metadata);
    }

    @Override
    public void saveEndringMetadata(String navident, Event.Metadata metadata, String type) {
        endringMetadataByNavident.put(navident, new MetadataWithType(metadata, type));
    }

    @Override
    public String findEndretAvByNavident(String navident) {
        var metadata = endringMetadataByNavident.get(navident);

        return metadata != null
            ? metadata.metadata().inputMetadata().origination().initiatedBy()
            : null;
    }

    @Override
    public LocalDateTime findEndretDatoByNavident(String navident) {
        var metadata = endringMetadataByNavident.get(navident);

        return metadata != null
            ? metadata.metadata().createdTimestamp()
            : null;
    }

    public String findEndringEventByNavident(String navident) {
         var metadata = endringMetadataByNavident.get(navident);

         return metadata != null
             ? metadata.type()
             : null;
    }

    @Override
    public String findOpprettetAvByNavident(String navident) {
        var metadata = opprettetMetadataByNavident.get(navident);

        return metadata != null
            ? metadata.inputMetadata().origination().initiatedBy()
            : null;
    }

    @Override
    public LocalDateTime findOpprettetDatoByNavident(String navident) {
        var metadata = opprettetMetadataByNavident.get(navident);

        return metadata != null
            ? metadata.createdTimestamp()
            : null;
    }

    @Override
    public int countOpprettelseMetadata() {
        return opprettetMetadataByNavident.size();
    }
}
