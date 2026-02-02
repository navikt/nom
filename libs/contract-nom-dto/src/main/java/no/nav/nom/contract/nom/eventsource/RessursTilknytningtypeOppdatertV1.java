package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.Tilknytningtype;

public class RessursTilknytningtypeOppdatertV1 extends RessursEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursTilknytningtypeOppdatertV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") TilknytningtypeOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursTilknytningtypeOppdatertV1(String fid, TilknytningtypeOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
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
