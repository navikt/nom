package no.nav.nom.apps.api.rest.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StandardResponse {
    private String message;
}
