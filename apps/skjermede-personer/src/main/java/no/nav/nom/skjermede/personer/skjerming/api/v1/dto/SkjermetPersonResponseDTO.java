package no.nav.nom.skjermede.personer.skjerming.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkjermetPersonResponseDTO {

    private String fornavn;
    private String etternavn;
    private String tilknyttetRessurs;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate skjermetFom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate skjermetTom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opprettetDato;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endretDato;

    public static SkjermetPersonResponseDTO fromEntity(final SkjermetPerson skjermetPerson) {
        return builder()
                .fornavn(skjermetPerson.getFornavn())
                .etternavn(skjermetPerson.getEtternavn())
                .skjermetFom(skjermetPerson.getSkjermetFom())
                .skjermetTom(skjermetPerson.getSkjermetTom())
                .tilknyttetRessurs(skjermetPerson.getTilknyttetRessurs())
                .opprettetDato(skjermetPerson.getOpprettetDato())
                .endretDato(skjermetPerson.getEndretDato())
                .build();
    }
}
