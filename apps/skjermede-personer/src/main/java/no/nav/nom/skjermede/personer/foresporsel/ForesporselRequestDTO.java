package no.nav.nom.skjermede.personer.foresporsel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
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
