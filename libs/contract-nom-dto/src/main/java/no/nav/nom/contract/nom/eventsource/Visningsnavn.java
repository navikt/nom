package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Optional;

public record Visningsnavn(
        String fornavn,
        Optional<String> mellomnavn,
        String etternavn
){
    public String toString(){
        return getFormattedVisningsnavn();
    }

    @JsonIgnore
    public String getFormattedVisningsnavn() {
        return fornavn + mellomnavn.map(it -> " " + it).orElse("") + (" " + etternavn);
    }

    @JsonIgnore
    public String getFornavnMellomnavn() {
        return fornavn + mellomnavn.map(it -> " " + it).orElse("");
    }

}
