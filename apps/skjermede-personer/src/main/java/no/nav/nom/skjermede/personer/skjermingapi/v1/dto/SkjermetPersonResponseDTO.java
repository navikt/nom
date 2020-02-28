package no.nav.nom.skjermede.personer.skjermingapi.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkjermetPersonResponseDTO implements Serializable {

    @JsonProperty
    private Long funksjonellId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate skjermetFom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate skjermetTom;

    @JsonProperty
    private String tilknyttetRessurs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opprettetDato;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endretDato;

    public static SkjermetPersonResponseDTO fromEntity(final SkjermetPerson skjermetPerson) {
        return builder()
                .funksjonellId(skjermetPerson.getFunksjonellId())
                .skjermetFom(skjermetPerson.getSkjermetFom())
                .skjermetTom(skjermetPerson.getSkjermetTom())
                .tilknyttetRessurs(skjermetPerson.getTilknyttetRessurs())
                .opprettetDato(skjermetPerson.getOpprettetDato())
                .endretDato(skjermetPerson.getEndretDato())
                .build();
    }
}
