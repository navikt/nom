package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NavidentResponse {

    private HttpStatus status;
    private String navident;
    private String feilmelding;
}
