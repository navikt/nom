package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDateTime;

public class RessursTilknytningtypeOppdatertV1 extends RessursEvent {
    @JsonCreator
    public RessursTilknytningtypeOppdatertV1(String fid, TilknytningtypeOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursTilknytningtypeOppdatertV1(String fid, TilknytningtypeOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record TilknytningtypeOppdatertEventData(
        Tilknytningtype tilknytningtype,

        // Må beholde denne fordi det finnes event med feltnavn 'sektor', etter at typo på feltet over.
        // Den het en liten periode 'Tilknytningtype sektor', Så for å fortsatt kunne parse alle RessursEvent, må denne ligge her.
        // Data for felt sektor fjernes i state streams, før de brukes som del av state
        String sektor
    ) implements Data{
        public TilknytningtypeOppdatertEventData(Tilknytningtype tilknytningtype) {
            this(tilknytningtype, null);
        }
    }
}
