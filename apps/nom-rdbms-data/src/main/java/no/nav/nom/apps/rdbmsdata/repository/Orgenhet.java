package no.nav.nom.apps.rdbmsdata.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDate;

@Entity
@Table(name = "orgenhet")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class Orgenhet {
    @Id
    @Column(name = "fid")
    private String fid;

    @Column(name = "id")
    private String id;

    @Column(name = "navn")
    private String navn;

    @Column(name = "gyldig_fom")
    private LocalDate gyldigFom;

    @Column(name = "gyldig_tom")
    private LocalDate gyldigTom;

    @Column(name = "orgenhet_type")
    @Enumerated(EnumType.STRING)
    private OrgEnhetsType orgenhetType;

    @Column(name = "nom_nivaa")
    @Enumerated(EnumType.STRING)
    private NomNivaa nomNivaa;

    @Column(name = "unique_agresso_id")
    private String uniqueAgressoId;

    @Column(name = "agresso_id")
    private String agressoId;

    @Column(name = "agresso_org_nivaa")
    private String agressoOrgNivaa;

    @Column(name = "agresso_orgenhet_type")
    private String agressoOrgenhetType;

    public Orgenhet(String fid) {
        this.fid = fid;
    }

    public static Orgenhet from(OrgEnhetState orgenhetState) {
        return Orgenhet.builder()
            .fid(orgenhetState.fid())
            .id(orgenhetState.id())
            .navn(orgenhetState.navn())
            .gyldigFom(orgenhetState.gyldigFom())
            .gyldigTom(orgenhetState.gyldigTom())
            .orgenhetType(orgenhetState.orgEnhetsType())
            .nomNivaa(orgenhetState.nomNivaa())
            .uniqueAgressoId(orgenhetState.uniqueAgressoId())
            .agressoId(orgenhetState.agressoId())
            .agressoOrgNivaa(orgenhetState.agressoOrgNivaa())
            .agressoOrgenhetType(orgenhetState.agressoOrgenhetType())
            .build();
    }
}
