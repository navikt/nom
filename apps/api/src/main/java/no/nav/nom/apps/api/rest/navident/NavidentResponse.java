package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NavidentResponse {

    private String status;
    private String personident;
    private String navident;
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private String feilmelding;
}
