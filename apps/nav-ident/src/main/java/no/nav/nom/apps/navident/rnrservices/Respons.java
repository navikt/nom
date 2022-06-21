package no.nav.nom.apps.navident.rnrservices;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Builder
@Data
@Slf4j
public class Respons {
    int code;
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
