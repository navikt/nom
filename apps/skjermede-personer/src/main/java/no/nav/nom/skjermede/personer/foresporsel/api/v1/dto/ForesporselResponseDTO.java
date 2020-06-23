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
public class ForesporselResponseDTO {

    private String tilknyttetRessurs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opprettetDato;

    public static ForesporselResponseDTO fromEntity(final Foresporsel foresporsel) {
        return builder().opprettetDato(foresporsel.getOpprettetDato()).tilknyttetRessurs(foresporsel.getTilknyttetRessurs()).build();
    }
}
