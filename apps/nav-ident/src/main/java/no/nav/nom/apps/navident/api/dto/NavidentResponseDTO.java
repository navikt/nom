package no.nav.nom.apps.navident.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NavidentResponseDTO {

    private String navident;
    private String feilmelding;
}
