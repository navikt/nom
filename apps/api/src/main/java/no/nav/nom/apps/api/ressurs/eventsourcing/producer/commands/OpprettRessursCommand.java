package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;

public record OpprettRessursCommand(
    String personident,
    String navident,
    Optional<Person> pdlPerson,
    Sektor sektor,
    Visningsnavn visningsnavn,
    Optional<String> primaryTelefonnummer,
    InputMetadata inputMetadata,
    LocalDate startdato,
    String orgEnhetId,
    AnsettelsesType ansettelsesType,
    String fysiskbrukerStedNr,
    IdentType identType
) implements RessursCommand {}
