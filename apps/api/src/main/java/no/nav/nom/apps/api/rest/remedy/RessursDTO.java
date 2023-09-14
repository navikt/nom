package no.nav.nom.apps.api.rest.remedy;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.apps.api.rest.dto.NavnDTO;

@Value
@Builder
public class RessursDTO {
    String personident;
    String navident;
    NavnDTO navn;
    String status;
    String feilmelding;
}

