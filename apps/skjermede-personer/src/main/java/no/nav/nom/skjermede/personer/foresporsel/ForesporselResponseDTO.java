package no.nav.nom.skjermede.personer.foresporsel;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForesporselResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opprettetDato;

    public static ForesporselResponseDTO fromEntity(final Foresporsel foresporsel) {
        return builder()
                .opprettetDato(foresporsel.getOpprettetDato())
                .build();
    }
}
