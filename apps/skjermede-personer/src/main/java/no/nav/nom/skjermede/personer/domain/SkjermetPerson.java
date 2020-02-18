package no.nav.nom.skjermede.personer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "T_SKJERMET_PERSON")
public class SkjermetPerson {

    @Id
    @Column(name = "PERSONIDENT")
    private String personident;

    @NotNull
    @Column(name = "FUNKSJONELL_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "funksjonellIdGenerator")
    @SequenceGenerator(name = "funksjonellIdGenerator", sequenceName = "T_SKJERMET_PERSON_FUNK_SEQ")
    private Long funksjonellId;

    @NotNull
    @Column(name = "SKJERMET_FOM")
    private LocalDate skjermetFom;

    @Column(name = "SKJERMET_TOM")
    private LocalDate skjermetTom;

    @NotNull
    @Column(name = "TILKNYTTET_RESSURS")
    private String tilknyttetRessurs;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDateTime opprettetDato;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDateTime endretDato;
}
