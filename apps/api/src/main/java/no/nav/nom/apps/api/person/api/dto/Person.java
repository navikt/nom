package no.nav.nom.apps.api.person.api.dto;

public record Person(
    String personident,
    String nomId,
    String navident,
    FolkeregisterPerson folkeregisterPerson
) {


}


