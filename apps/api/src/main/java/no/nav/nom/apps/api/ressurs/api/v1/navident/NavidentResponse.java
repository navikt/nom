package no.nav.nom.apps.api.ressurs.api.v1.navident;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class NavidentResponse {

    private String status;
    private String personident;
    private String navident;
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private String feilmelding;


    public static NavidentResponse medFeilmelding(HttpStatusCode httpStatusCode, String feilmelding) {
        return medFeilmelding(Integer.toString(httpStatusCode.value()), feilmelding);
    }

    public static NavidentResponse medFeilmelding(String status, String feilmelding) {
        log.error("Feil ved opprettNavident: {} - {}", status, feilmelding);
        return NavidentResponse.builder()
                .status(status)
                .feilmelding(feilmelding)
                .build();
    }
}
