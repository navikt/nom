package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

@Data
@Builder
public class OpprettRessursRequest {

    private String personident;
    private String visningsfornavn;
    private String visningsmellomnavn;
    private String visningsetternavn;
    private Sektor sektor;
    private String orgEnhetId;
    private LocalDate startDato;
    private AnsettelsesType ansettelsesType;
}
