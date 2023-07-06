package no.nav.nom.apps.api.rest.remedy;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.apps.api.rest.dto.NavnDTO;

@Value
@Builder
public class RessursDTO {
    NavnDTO navn;
    String status;
    String feilmelding;
}

