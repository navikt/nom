package no.nav.nom.apps.nom.ressurser.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "T_RESSURS")
public class Ressurs {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ressursIdGenerator")
    @SequenceGenerator(name = "ressursIdGenerator", sequenceName = "T_RESSURS_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "PERSONIDENT")
    private String personident;

    @Column(name = "RESSURS_TYPE")
    @Enumerated(EnumType.STRING)
    private RessursType ressursType;

    @NotNull
    @Column(name = "STARTDATO")
    private LocalDate startDato;

    @Column(name = "SLUTTDATO")
    private LocalDate sluttdato;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDate opprettetDato;

    @Column(name = "ENDRET_DATO")
    private LocalDate endretDato;
}
