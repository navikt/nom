package no.nav.nom.apps.api.common;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RessursStateBuilder {
    private String fid;
    private String personident;
    private String navident;
    private Sektor sektor;
    private Visningsnavn visningsnavn;
    private Jobbtelefon jobbtelefon;
    private String primaryTelefonnummer;
    private LocalDate sluttdato;

    private LocalDate startdato;

    private String primaryFysiskBrukersted;
    private List<String> fysiskeBrukersteder;

    public RessursStateBuilder fid(String fid) {
        this.fid = fid;
        return this;
    }

    public RessursStateBuilder personident(String personident) {
        this.personident = personident;
        return this;
    }

    public RessursStateBuilder navident(String navident) {
        this.navident = navident;
        return this;
    }

    public RessursStateBuilder sektor(Sektor sektor) {
        this.sektor = sektor;
        return this;
    }

    public RessursStateBuilder visningsnavn(Visningsnavn visningsnavn) {
        this.visningsnavn = visningsnavn;
        return this;
    }

    public RessursStateBuilder jobbtelefon(Jobbtelefon jobbtelefon) {
        this.jobbtelefon = jobbtelefon;
        return this;
    }

    public RessursStateBuilder primaryTelefonnummer(String primaryTelefonnummer) {
        this.primaryTelefonnummer = primaryTelefonnummer;
        return this;
    }

    public RessursStateBuilder visningsnavn(String fornavn, String etternavn) {
        this.visningsnavn = new Visningsnavn(
            fornavn, Optional.empty(), etternavn
        );
        return this;
    }

    public RessursStateBuilder sluttdato(LocalDate sluttdato) {
        this.sluttdato = sluttdato;
        return this;
    }

    public RessursStateBuilder startdato(LocalDate startdato) {
        this.startdato = startdato;
        return this;
    }

    public RessursStateBuilder primaryFysiskBrukersted(String primaryFysiskBrukersted) {
        this.primaryFysiskBrukersted = primaryFysiskBrukersted;
        return this;
    }

    public RessursStateBuilder fysiskeBrukersteder(List<String> fysiskeBrukersteder) {
        this.fysiskeBrukersteder = fysiskeBrukersteder;
        return this;
    }

    public RessursStateBuilder(int suffix) {
        this.fid = "fid" + suffix;
        this.personident = "pid" + suffix;
        this.navident = "A000000".substring(0, 7 - String.valueOf(suffix).length()) + suffix;
        this.sektor = Sektor.NAV_STATLIG;
        this.visningsnavn = new Visningsnavn(
            "fornavn" + suffix,
            Optional.empty(),
            "etternavn" + suffix
        );
        this.jobbtelefon = new Jobbtelefon(
            "12345678" + suffix,
            TelefonType.NAV_TJENESTE_TELEFON
        );
        this.primaryTelefonnummer = null;
        this.sluttdato = null;
        this.startdato = null;
        this.primaryFysiskBrukersted = null;
        this.fysiskeBrukersteder = Collections.emptyList();

    }

    public RessursState build() {
        return new RessursState(fid, personident, navident, sektor, visningsnavn, jobbtelefon, primaryTelefonnummer, sluttdato, startdato, primaryFysiskBrukersted, fysiskeBrukersteder, IdentType.STANDARD, null);
    }

}
