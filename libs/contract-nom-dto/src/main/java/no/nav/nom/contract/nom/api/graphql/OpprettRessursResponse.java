package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.Sektor;

@Data
@Builder
public class OpprettRessursResponse {

    private String personident;
    private String navident;
    private String visningsfornavn;
    private String visningsetternavn;
    private Sektor sektor;

}
