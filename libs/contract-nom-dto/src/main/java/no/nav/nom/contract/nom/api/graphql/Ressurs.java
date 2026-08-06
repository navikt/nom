package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.List;

@EqualsAndHashCode()
@Builder(toBuilder = true)
public class Ressurs {
    @Deprecated    String navIdent;
    @Deprecated    String personIdent;
    @Deprecated    String visningsNavn;
    @Deprecated    List<Sektor> sektor;

    String personident;
    String navident;
    LocalDate sluttdato;
    LocalDate startdato;
    Sektor gjeldendeSektor;
    IdentType identType;
    String fornavn;
    String etternavn;
    String visningsnavn;
    String primaryTelefon;
    List<Telefon> telefon;
    String primaryFysiskBrukersted;
    List<String> additionalFysiskeBrukerstederNummer;

    @Deprecated
    public String getNavIdent() {
        return navIdent;
    }

    @Deprecated
    public String getPersonIdent() {
        return personIdent;
    }

    @Deprecated
    public String getVisningsNavn() {
        return visningsNavn;
    }

    @Deprecated
    public List<Sektor> getSektor() {
        return sektor;
    }

    public String getPersonident() {
        return personident;
    }

    public String getNavident() {
        return navident;
    }

    public LocalDate getSluttdato() {
        return sluttdato;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    public Sektor getGjeldendeSektor() {
        return gjeldendeSektor;
    }

    public IdentType getIdentType() {
        return identType;
    }

    public String getFornavn() {
        return fornavn;
    }

    public String getEtternavn() {
        return etternavn;
    }

    public String getVisningsnavn() {
        return visningsnavn;
    }

    public String getPrimaryTelefon() {
        return primaryTelefon;
    }

    public List<Telefon> getTelefon() {
        return telefon;
    }

    public Tilknytningtype getGjeldendeTilknytningtype() {
        return gjeldendeTilknytningtype;
    }

    Tilknytningtype gjeldendeTilknytningtype;

    public String getPrimaryFysiskBrukersted() { return primaryFysiskBrukersted; }

    public List<String> getAdditionalFysiskeBrukerstederNummer() { return additionalFysiskeBrukerstederNummer; }
}