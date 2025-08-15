package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1;

import lombok.Builder;
import lombok.Getter;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;

@Getter
public final class RessursOpprettetEvent extends RessursEvent {
    private final String fid;
    private final Visningsnavn visningsnavn;
    private final LocalDate startdato;
    private final LocalDate sluttdato;
    private final Sektor sektor;
    private final Orgtilknytning orgtilknytning;

    @Builder
    public RessursOpprettetEvent(
        String navident,
        String fid,
        Visningsnavn visningsnavn,
        LocalDate startdato,
        LocalDate sluttdato,
        Sektor sektor,
        Orgtilknytning orgtilknytning
    ) {
        super("RessursOpprettetEvent", navident);

        this.fid = fid;
        this.visningsnavn = visningsnavn;
        this.startdato = startdato;
        this.sluttdato = sluttdato;
        this.sektor = sektor;
        this.orgtilknytning = orgtilknytning;
    }

}
