package no.nav.nom.skjermede.personer.foresporsel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import org.hibernate.annotations.GenericGenerator;

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
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_FORESPORSEL")
public class Foresporsel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "foresporselIdGenerator")
    @SequenceGenerator(name = "foresporselIdGenerator", sequenceName = "T_FORESPORSEL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "FUNKSJONELL_ID")
    private UUID funksjonellId;

    @NotNull
    @Column(name = "PERSONIDENT")
    private String personident;

    @NotNull
    @Column(name = "FORNAVN")
    private String fornavn;

    @NotNull
    @Column(name = "ETTERNAVN")
    private String etternavn;

    @Column(name = "TILKNYTTET_RESSURS")
    private String tilknyttetRessurs;

    @Column(name = "EPOST_SENDT")
    private LocalDateTime epostSendt;

    @Column(name = "BEKREFTET")
    private Boolean bekreftet;

    @Column(name = "BEKREFTET_DATO")
    private LocalDateTime bekreftetDato;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDateTime opprettetDato;
}
