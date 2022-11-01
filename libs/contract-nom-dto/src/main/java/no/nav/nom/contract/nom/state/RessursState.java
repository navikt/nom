package no.nav.nom.contract.nom.state;

import java.util.UUID;

public record RessursState(
        UUID fid,
        String personident,
        String navident,
        Sektor sektor,
        Visningsnavn visningsnavn
) {

    public record Visningsnavn(
            String fornavn,
            String mellomnavn,
            String etternavn
    ){}

    public enum Sektor{
        NAV_STATLIG,
        NAV_KOMMUNAL,
        EKSTERN
    }
}
