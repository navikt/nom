package no.nav.nom.apps.rdbmsdata.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "orgtilknytning")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class Orgtilknytning {
    @Id
    @Column(name = "fid")
    private String fid;

    @Column(name = "navident")
    private String navident;

    @Column(name = "orgenhet_id")
    private String orgEnhetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sektor")
    private Sektor sektor;

    @Column(name = "er_daglig_oppfolging")
    private boolean erDagligOppfolging;

    @Column(name = "gyldig_fom")
    private LocalDate gyldigFom;

    @Column(name = "gyldig_tom")
    private LocalDate gyldigTom;

    @Enumerated(EnumType.STRING)
    @Column(name = "orgtilknytning_type")
    private Tilknytningtype orgtilknytningType;

    @Column(name = "primary_fysisk_brukersted_nummer")
    private String primaryFysiskBrukerstedNummer;

    @Column(name = "additional_fysiske_brukersteder_nummer")
    @JdbcTypeCode(SqlTypes.ARRAY)
    String[] additionalFysiskeBrukerstederNummer;

    @Column(name = "masseflyttet_fra_fid")
    private String masseflyttetFraFid;

    @Column(name = "sist_endret_av")
    private String sistEndretAv;

    public Orgtilknytning(String fid) {
        this.fid = fid;
    }

    public static Orgtilknytning from(OrgtilknytningState orgtilknytningState) {
        return Orgtilknytning.builder()
            .fid(orgtilknytningState.fid())
            .navident(orgtilknytningState.navident())
            .orgEnhetId(orgtilknytningState.orgEnhetId())
            .sektor(orgtilknytningState.sektor())
            .erDagligOppfolging(orgtilknytningState.erDagligOppfolging())
            .gyldigFom(orgtilknytningState.gyldigFom())
            .gyldigTom(orgtilknytningState.gyldigTom())
            .orgtilknytningType(orgtilknytningState.orgtilknytningType())
            .primaryFysiskBrukerstedNummer(orgtilknytningState.primaryFysiskBrukerstedNummer())
            .additionalFysiskeBrukerstederNummer(orgtilknytningState.additionalFysiskeBrukerstederNummer().toArray(String[]::new))
            .masseflyttetFraFid(orgtilknytningState.masseflyttetFraFid())
            .sistEndretAv(orgtilknytningState.sistEndretAv())
            .build();
    }
}