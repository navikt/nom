package no.nav.nom.skjermede.personer.skjermingapi.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;

import java.time.LocalDate;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkjermetPersonRequestDTO {

    private String personident;
    private String navIdent;
    private String fornavn;
    private String etternavn;
    private LocalDate skjermetFom;
    private LocalDate skjermetTom;
    private String tilknyttetRessurs;

    public SkjermetPerson toEntity() {
        return SkjermetPerson.builder()
                .personident(personident)
                .navIdent(navIdent)
                .fornavn(fornavn)
                .etternavn(etternavn)
                .skjermetFom(skjermetFom)
                .skjermetTom(skjermetTom)
                .tilknyttetRessurs(tilknyttetRessurs)
                .build();
    }
}
