package no.nav.nom.apps.nom.unit4.kafka;

import lombok.Value;

@Value
public class NomRessursDto {
    String personident;
    String navident;
    String visningsfornavn;
    String visningsmellomnavn;
    String visningsetternavn;
    String sektor;
    String kjoenn;
}
