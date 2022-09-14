package no.nav.nom.apps.nom.unit4.kafka;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
public class NomRessursDto {
    String personident;
    String navident;
    String visningsfornavn;
    String visningsmellomnavn;
    String visningsetternavn;
    String sektor;
    String kjoenn;
}
