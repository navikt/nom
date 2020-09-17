package no.nav.nom.skjermede.personer.foresporsel.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TilknyttetForesporselDTO {

    private String fornavn;

    private String etternavn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opprettetDato;

    public static TilknyttetForesporselDTO fromEntity(final Foresporsel foresporsel) {
        return builder()
                .opprettetDato(foresporsel.getOpprettetDato())
                .fornavn(foresporsel.getFornavn())
                .etternavn(foresporsel.getEtternavn())
                .build();
    }
}
