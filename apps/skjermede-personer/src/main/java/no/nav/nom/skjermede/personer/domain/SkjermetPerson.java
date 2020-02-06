package no.nav.nom.skjermede.personer.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "T_SKJERMET_PERSON")
public class SkjermetPerson {

    @Id
    @Column(name = "PERSONIDENT")
    private String personident;

    @NotNull
    @Column(name = "SKJERMET_FOM")
    private LocalDate skjermetFom;

    @Column(name = "SKJERMET_TOM")
    private LocalDate skjermetTom;

    @Column(name = "TILKNYTTET_PERSON")
    private String tilknyttetRessurs;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDateTime opprettetDato;

    @Column(name = "ENDRET_DATO")
    private LocalDateTime endretDato;
}
