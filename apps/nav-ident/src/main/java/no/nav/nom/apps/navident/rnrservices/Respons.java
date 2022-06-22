package no.nav.nom.apps.navident.rnrservices;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Builder
@Data
@Slf4j
public class Respons {
    HttpStatus httpStatus;
    Body body;

    @Data
    @Builder
    public static class Body {
        String navident;
        LocalDate aktivFra;
        LocalDate aktivTil;
        String feilmelding;
    }

}
