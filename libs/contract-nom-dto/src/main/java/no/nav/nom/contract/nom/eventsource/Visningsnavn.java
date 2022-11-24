package no.nav.nom.contract.nom.eventsource;

import java.util.Optional;

public record Visningsnavn(
        String fornavn,
        Optional<String> mellomnavn,
        String etternavn
){
    public String toString(){
        return fornavn + " " + mellomnavn.map(it -> " " + it).orElse("") + " " + etternavn;
    }

}
