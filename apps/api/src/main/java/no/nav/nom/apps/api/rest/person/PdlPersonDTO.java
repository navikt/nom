package no.nav.nom.apps.api.rest.person;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.apps.api.rest.dto.NavnDTO;

@Value
@Builder
public class PdlPersonDTO {
    NavnDTO navn;
}