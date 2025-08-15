package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1;

import lombok.Getter;

@Getter
public class RessursEvent {
    private final String eventtype;
    private final String navident;

    public RessursEvent(
        String eventtype,
        String navident
    ) {
        this.eventtype = eventtype;
        this.navident = navident;
    }
}
