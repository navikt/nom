package no.nav.nom.apps.api.rest.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NavnDTO {
    String fornavn;
    String mellomnavn;
    String etternavn;
}
