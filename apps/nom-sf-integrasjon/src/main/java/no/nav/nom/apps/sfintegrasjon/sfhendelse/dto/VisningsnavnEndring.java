package no.nav.nom.apps.sfintegrasjon.sfhendelse.dto;


public record VisningsnavnEndring(
    String fornavn,
    String etternavn
) implements HendelseDiff {}
