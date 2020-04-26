package no.nav.nom.skjermede.personer.foresporsel.api.v1;

import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;
import no.nav.nom.skjermede.personer.foresporsel.dto.ForesporselRequestDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public class ForesporselTestUtils {

    public static final String PERSONIDENT = "12345678910";
    public static final String TILKNYTTET_RESSURS = "A000000";

    public static Foresporsel foresporsel() {
        return Foresporsel.builder()
                .funksjonellId(UUID.randomUUID())
                .personident(PERSONIDENT)
                .fornavn("Jenny")
                .etternavn("Doe")
                .tilknyttetRessurs(TILKNYTTET_RESSURS)
                .opprettetDato(LocalDateTime.now())
                .build();
    }

    public static ForesporselRequestDTO foresporselRequest() {
        return ForesporselRequestDTO.builder()
                .fornavn("Jenny")
                .etternavn("Doe")
                .tilknyttetRessurs(TILKNYTTET_RESSURS)
                .build();
    }
}
