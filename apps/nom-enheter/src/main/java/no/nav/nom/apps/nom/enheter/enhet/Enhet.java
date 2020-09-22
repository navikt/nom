package no.nav.nom.apps.nom.enheter.enhet;

import com.sun.istack.NotNull;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ENHET")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enhet {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enhetIdGenerator")
    @SequenceGenerator(name = "enhetIdGenerator", sequenceName = "T_ENHET_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "ENHETSNR")
    private Long enhetsnr;

    @NotNull
    @Column(name = "NAVN")
    private String navn;

    @NotNull
    @Column(name = "ENHETSTYPE")
    private String enhetstype;

    @NotNull
    @Column(name = "AGRESSO_NR")
    private String agressoNr;

    @NotNull
    @Column(name = "REMEDY_NR")
    private String remedyNr;

    @NotNull
    @Column(name = "GYLDIG_FRA")
    private LocalDate gyldigFra;

    @Column(name = "GYLDIG_TIL")
    private LocalDate gyldigTil;

    @NotNull
    @Column(name = "OPPRETTET_AV")
    private String opprettetAv;

    @NotNull
    @Column(name = "OPPRETTETDATO")
    private LocalDateTime opprettetDato;

    @NotNull
    @Column(name = "ENDRET_AV")
    private String endretAv;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDateTime endretDato;
}
