package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

public record Visningsnavn(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fornavn,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<String> mellomnavn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String etternavn
){
    public String toString(){
        return getFormattedVisningsnavn();
    }

    @JsonIgnore
    public String getFormattedVisningsnavn() {
        return fornavn + mellomnavn.map(it -> it.isBlank() ? "" : " " + it).orElse("") + (" " + etternavn);
    }

    @JsonIgnore
    public String getFornavnMellomnavn() {
        return fornavn + mellomnavn.map(it -> it.isBlank() ? "" : " " + it).orElse("");
    }

}
