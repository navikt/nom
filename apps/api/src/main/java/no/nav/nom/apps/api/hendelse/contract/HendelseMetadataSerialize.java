package no.nav.nom.apps.api.hendelse.contract;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;


@Value
public class HendelseMetadataSerialize extends HendelseMetadata{

    private JsonNode rawInputData;

    @Builder
    public HendelseMetadataSerialize(OpprettetAv opprettetAv, LocalDateTime opprettetTidspunkt, JsonNode rawInputData) {
        super(opprettetAv, opprettetTidspunkt);
        this.rawInputData = rawInputData;
    }
}
