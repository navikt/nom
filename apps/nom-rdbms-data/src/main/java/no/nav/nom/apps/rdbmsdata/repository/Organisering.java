package no.nav.nom.apps.rdbmsdata.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.nom.contract.nom.state.OrganiseringState;

import java.time.LocalDate;

@Entity
@Table(name = "organisering")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class Organisering {
    @Id
    @Column(name = "fid")
    private String fid;

    @Column(name = "id")
    private String id;

    @Column(name = "navn")
    private String navn;

    @Column(name = "id_over")
    private String idOver;

    @Column(name = "navn_over")
    private String navnOver;

    @Column(name = "gyldig_fom")
    private LocalDate gyldigFom;

    @Column(name = "gyldig_tom")
    private LocalDate gyldigTom;

    public Organisering(String fid) {
        this.fid = fid;
    }

    public static Organisering from(OrganiseringState organiseringState) {
        return Organisering.builder()
            .fid(organiseringState.fid())
            .id(organiseringState.id())
            .navn(organiseringState.navn())
            .idOver(organiseringState.idOver())
            .navnOver(organiseringState.navnOver())
            .gyldigFom(organiseringState.gyldigFom())
            .gyldigTom(organiseringState.gyldigTom())
            .build();
    }
}
