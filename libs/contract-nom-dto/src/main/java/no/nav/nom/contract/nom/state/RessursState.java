package no.nav.nom.contract.nom.state;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record RessursState(
        UUID fid,
        String personident,
        String navident,
        Sektor sektor,
        Visningsnavn visningsnavn,
        Optional<LocalDate> sluttdato
) {

    public record Visningsnavn(
            String fornavn,
            Optional<String> mellomnavn,
            String etternavn
    ){}

    public enum Sektor{
        NAV_STATLIG,
        NAV_KOMMUNAL,
        EKSTERN
    }
}
