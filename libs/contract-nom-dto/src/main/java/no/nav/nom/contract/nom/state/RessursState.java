package no.nav.nom.contract.nom.state;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;

public record RessursState(
    String fid,
    String personident,
    String navident,
    Sektor sektor,
    Visningsnavn visningsnavn,
    Optional<LocalDate> sluttdato
) {

    public RessursState(String fid, String personident, String navident, Sektor sektor, Visningsnavn visningsnavn, Optional<LocalDate> sluttdato) {
        this.fid = fid;
        this.personident = personident;
        this.navident = navident;
        this.sektor = sektor;
        this.visningsnavn = visningsnavn;
        this.sluttdato = sluttdato;
    }

    public static class Builder {
        private String fid;
        private String personident;
        private String navident;
        private Sektor sektor;
        private Visningsnavn visningsnavn;
        private Optional<LocalDate> sluttdato;

        public Builder fid(String fid) {
            this.fid = fid;
            return this;
        }

        public Builder personident(String personident) {
            this.personident = personident;
            return this;
        }

        public Builder navident(String navident) {
            this.navident = navident;
            return this;
        }

        public Builder sektor(Sektor sektor) {
            this.sektor = sektor;
            return this;
        }

        public Builder visningsnavn(Visningsnavn visningsnavn) {
            this.visningsnavn = visningsnavn;
            return this;
        }

        public Builder visningsnavn(String fornavn, String etternavn) {
            this.visningsnavn = new Visningsnavn(
                fornavn,null,etternavn
            );
            return this;
        }

        public Builder sluttdato(Optional<LocalDate> sluttdato) {
            this.sluttdato = sluttdato;
            return this;
        }

        public Builder(int suffix) {
            this.fid = "fid" + suffix;
            this.personident = "pid" + suffix;
            this.navident = "A000000".substring(0,7-String.valueOf(suffix).length()) + suffix;
            this.sektor = Sektor.NAV_STATLIG;
            this.visningsnavn = new Visningsnavn(
                "fornavn" + suffix,
                null,
                "etternavn" + suffix
            );
            this.sluttdato = Optional.empty();

        }

        public RessursState build() {
            return new RessursState(fid, personident, navident, sektor, visningsnavn, sluttdato);
        }

    }
}
