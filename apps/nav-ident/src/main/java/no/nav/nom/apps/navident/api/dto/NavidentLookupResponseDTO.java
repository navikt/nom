package no.nav.nom.apps.navident.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.IdentType;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NavidentLookupResponseDTO {
    private String navident;
    private IdentType identType;
}
