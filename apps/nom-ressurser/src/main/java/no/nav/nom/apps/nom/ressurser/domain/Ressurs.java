package no.nav.nom.apps.nom.ressurser.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.contract.nom.dto.Ressurstype;

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

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "T_RESSURS")
public class Ressurs {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ressursIdGenerator")
    @SequenceGenerator(name = "ressursIdGenerator", sequenceName = "T_RESSURS_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "FUNKSJONELL_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "funksjonellIdGenerator")
    @SequenceGenerator(name = "funksjonellIdGenerator", sequenceName = "T_RESSURS_FUNK_SEQ")
    private Long funksjonellId;

    @NotNull
    @Column(name = "PERSONIDENT")
    private String personident;

    @NotNull
    @Column(name = "RESSURSTYPE")
    @Enumerated(EnumType.STRING)
    private Ressurstype ressurstype;

    @NotNull
    @Column(name = "STARTDATO")
    private LocalDate startdato;

    @Column(name = "SLUTTDATO")
    private LocalDate sluttdato;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDate opprettetDato;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDate endretDato;
}
