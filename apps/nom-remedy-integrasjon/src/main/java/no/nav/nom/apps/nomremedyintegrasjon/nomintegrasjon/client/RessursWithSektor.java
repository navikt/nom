package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client;

import no.nav.nom.contract.nom.Sektor;

import java.util.List;

public record RessursWithSektor (
  String navident,
  List<Sektor> sektor
){}
