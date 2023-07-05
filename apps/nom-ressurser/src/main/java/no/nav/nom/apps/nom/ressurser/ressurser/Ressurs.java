package no.nav.nom.apps.nom.ressurser.ressurser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.contract.nom.dto.Ressurstype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_RESSURS")
public class Ressurs {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ressursIdGenerator")
    @SequenceGenerator(name = "ressursIdGenerator", sequenceName = "T_RESSURS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAVIDENT")
    private String navident;

    @Column(name = "EPOST")
    private String epost;

    @NotNull
    @Column(name = "FORNAVN")
    private String fornavn;

    @NotNull
    @Column(name = "ETTERNAVN")
    private String etternavn;

    @NotNull
    @Column(name = "PERSONIDENT")
    private String personident;

    @NotNull
    @Column(name = "RESSURSTYPE")
    @Enumerated(EnumType.STRING)
    private Ressurstype ressurstype;

    @NotNull
    @Column(name = "PERMISJON")
    private Boolean permisjon;

    @NotNull
    @Column(name = "STARTDATO")
    private LocalDate startdato;

    @Column(name = "SLUTTDATO")
    private LocalDate sluttdato;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDate endretDato;

    @NotNull
    @Column(name = "OPPRETTET_DATO")
    private LocalDate opprettetDato;

    @Column(name = "PRODUCED")
    private boolean produced;
}
