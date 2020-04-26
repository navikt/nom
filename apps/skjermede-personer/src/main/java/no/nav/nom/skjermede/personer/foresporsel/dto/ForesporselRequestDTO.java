package no.nav.nom.skjermede.personer.foresporsel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForesporselRequestDTO {

    private String fornavn;
    private String etternavn;
    private String tilknyttetRessurs;

    public Foresporsel toEntity() {
        return Foresporsel.builder()
                .fornavn(fornavn)
                .etternavn(etternavn)
                .tilknyttetRessurs(tilknyttetRessurs)
                .build();
    }
}
