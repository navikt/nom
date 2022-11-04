package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

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


    public static NavidentResponse medFeilmelding(HttpStatus status, String feilmelding) {
        medFeilmelding(Integer.toString(status.value()), feilmelding);
    }

    public static NavidentResponse medFeilmelding(String status, String feilmelding) {
        return NavidentResponse.builder()
                .status(status)
                .feilmelding(feilmelding)
                .build();
    }
}
