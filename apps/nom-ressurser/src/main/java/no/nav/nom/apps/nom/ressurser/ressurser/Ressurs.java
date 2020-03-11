package no.nav.nom.apps.nom.ressurser.ressurser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.contract.nom.dto.Ressurstype;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_RESSURS")
public class Ressurs {

    @Id
    @Column(name = "NAV_IDENT")
    private String navIdent;

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
    @Column(name = "STARTDATO")
    private LocalDate startdato;

    @Column(name = "SLUTTDATO")
    private LocalDate sluttdato;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDate endretDato;

    @Column(name = "PRODUCED")
    private boolean produced;
}
