package no.nav.nom.apps.rdbmsdata.repository;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.RessursState;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "ressurs")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class Ressurs {
    @Id
    @Column(name = "fid")
    private String fid;

    @Column(name = "personident")
    private String personident;

    @Column(name = "navident")
    private String navident;

    @Column(name = "sektor")
    @Enumerated(EnumType.STRING)
    private Sektor sektor;

    @Column(name = "fornavn")
    private String fornavn;

    @Column(name = "mellomnavn")
    private String mellomnavn;

    @Column(name = "etternavn")
    private String etternavn;

    @Column(name = "visningsnavn")
    private String visningsnavn;

    @Column(name = "primary_telefonnummer")
    private String primaryTelefonnummer;

    @Column(name = "startdato")
    private LocalDate startdato;

    @Column(name = "sluttdato")
    private LocalDate sluttdato;

    @Column(name = "primary_fysisk_brukersted")
    private String primaryFysiskBrukersted;

    @Column(name = "fysiske_brukersteder")
    @Type(StringArrayType.class)
    private String[] fysiskeBrukersteder;

    @Enumerated(EnumType.STRING)
    @Column(name = "ident_type")
    private IdentType identType;

    @Column(name = "sist_endret_av")
    private String sistEndretAv;

    public Ressurs(String fid) {
        this.fid = fid;
    }

    public static Ressurs from(RessursState ressursState) {
        return Ressurs.builder()
            .fid(ressursState.fid())
            .personident(ressursState.personident())
            .navident(ressursState.navident())
            .sektor(ressursState.sektor())
            .fornavn(ressursState.visningsnavn().fornavn())
            .mellomnavn(ressursState.visningsnavn().mellomnavn().orElse(null))
            .etternavn(ressursState.visningsnavn().etternavn())
            .visningsnavn(ressursState.visningsnavn().getFormattedVisningsnavn())
            .primaryTelefonnummer(ressursState.primaryTelefonnummer().orElse(null))
            .startdato(ressursState.startdato().orElse(null))
            .sluttdato(ressursState.sluttdato().orElse(null))
            .primaryFysiskBrukersted(ressursState.primaryFysiskBrukersted().orElse(null))
            .fysiskeBrukersteder(ressursState.fysiskeBrukersteder().toArray(String[]::new))
            .identType(ressursState.identType())
            .sistEndretAv(ressursState.sistEndretAv())
            .build();
    }
}
