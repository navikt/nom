package no.nav.nom.apps.sfintegrasjon.sfhendelse.dto;

import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;

public record NyRessurs(
    String personident,
    String visningsfornavn,
    String visningsetternavn,
    Sektor sektor,
    String orgEnhetId,
    LocalDate startDato,
    LocalDate sluttDato,
    AnsettelsesType ansettelsesType,
    String fysiskBrukerstedNr,
    Tilknytningtype tilknytningtype,
    IdentType identType

) implements HendelseDiff {}

