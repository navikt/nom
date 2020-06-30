package no.nav.nom.skjermede.personer.skjerming.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkjermetPersonDataResponseDTO {

    private String personident;
    private String fornavn;
    private String etternavn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime skjermetFra;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime skjermetTil;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opprettetDato;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endretDato;

    public static SkjermetPersonDataResponseDTO fromEntity(final SkjermetPerson skjermetPerson) {
        return builder()
                .personident(skjermetPerson.getPersonident())
                .fornavn(skjermetPerson.getFornavn())
                .etternavn(skjermetPerson.getEtternavn())
                .skjermetFra(skjermetPerson.getSkjermetFra())
                .skjermetTil(skjermetPerson.getSkjermetTil())
                .opprettetDato(skjermetPerson.getOpprettetDato())
                .endretDato(skjermetPerson.getEndretDato())
                .build();
    }
}
