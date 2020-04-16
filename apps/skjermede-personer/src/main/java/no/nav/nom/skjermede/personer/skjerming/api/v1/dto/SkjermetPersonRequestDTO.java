package no.nav.nom.skjermede.personer.skjerming.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkjermetPersonRequestDTO {

    private String fornavn;
    private String etternavn;
    private LocalDateTime skjermetFra;
    private LocalDateTime skjermetTil;
    private String tilknyttetRessurs;

    public SkjermetPerson toEntity() {
        return SkjermetPerson.builder()
                .fornavn(fornavn)
                .etternavn(etternavn)
                .skjermetFra(skjermetFra)
                .skjermetTil(skjermetTil)
                .tilknyttetRessurs(tilknyttetRessurs)
                .build();
    }
}
