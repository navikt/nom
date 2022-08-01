package no.nav.nom.apps.api.graphql.ressurs;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.api.graphql.Sektor;

@Data
@Builder
public class OpprettRessursResponse0 {

    private String personident;
    private String navident;
    private String visningsfornavn;
    private String visningsetternavn;
    private Sektor sektor;

}
