package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

public record ImportedRemedyUser(
    String navident,
    String fornavn,
    String etternavn,
    String enhetsId,
    String enhetsNavn,
    Sektor sektor,
    boolean aktiv,
    boolean permisjon,
    LocalDate sluttDato,
    LocalDate utlopsDato,
    LocalDate regDato
) {
    public String getVisningsnavn() { return fornavn + " " + etternavn; }
}
