package no.nav.nom.skjermede.personer.skjerming;

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
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_SKJERMET_PERSON")
public class SkjermetPerson {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGenerator")
    @SequenceGenerator(name = "idGenerator", sequenceName = "T_SKJERMET_PERSON_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "PERSONIDENT")
    private String personident;

    @NotNull
    @Column(name = "FORNAVN")
    private String fornavn;

    @NotNull
    @Column(name = "ETTERNAVN")
    private String etternavn;

    @NotNull
    @Column(name = "SKJERMET_FRA")
    private LocalDateTime skjermetFra;

    @Column(name = "SKJERMET_TIL")
    private LocalDateTime skjermetTil;

    @Column(name = "TILKNYTTET_RESSURS")
    private String tilknyttetRessurs;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDateTime opprettetDato;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDateTime endretDato;

    @Column(name = "ENDRING_SENDT_TPS")
    private LocalDateTime endringSendtTps;

    @Column(name = "OPPHOR_SENDT_TPS")
    private LocalDateTime opphorSendtTps;

    @Column(name = "KAFKA_PRODUCED")
    private boolean kafkaProduced;
}
