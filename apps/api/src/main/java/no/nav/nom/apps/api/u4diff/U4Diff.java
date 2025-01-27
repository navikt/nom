package no.nav.nom.apps.api.u4diff;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotNull;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.contract.nom.Sektor;
import org.hibernate.type.YesNoConverter;

import java.time.LocalDate;

@Data
@Entity
@Builder
@Table(name = "T_U4_DIFF")
@AllArgsConstructor
@NoArgsConstructor
public class U4Diff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private String navident;

    @Enumerated(EnumType.STRING)
    @Column(name = "nom_sektor")
    private Sektor nomSektor;

    @Enumerated(EnumType.STRING)
    @Column(name = "kilde")
    private U4DiffKilde kilde;

    @Enumerated(EnumType.STRING)
    private U4DiffType type;

    @Column(name = "u4_orgenhet_id")
    private String u4OrgenhetId;

    @Column(name = "u4_orgenhet_navn")
    private String u4OrgenhetNavn;

    @Column(name = "u4_gyldig_fom")
    private LocalDate u4GyldigFom;

    @Column(name = "u4_gyldig_tom")
    private LocalDate u4GyldigTom;

    @Column(name = "u4_dol")
    @Convert(converter = YesNoConverter.class)
    private Boolean u4Dol;

    @Column(name = "u4_sektor")
    @Enumerated(EnumType.STRING)
    private Sektor u4Sektor;

    @Column(name = "u4_stilling_periode")
    private String u4StillingPeriode;

    @Column(name = "u4_ansettelsestype_besk")
    private String u4AnsettelsestypeBesk;

    @Column(name = "nom_orgenhet_id")
    private String nomOrgenhetId;

    @Column(name = "nom_orgenhet_navn")
    private String nomOrgenhetNavn;

    @Column(name = "nom_gyldig_fom")
    private LocalDate nomGyldigFom;

    @Column(name = "nom_gyldig_tom")
    private LocalDate nomGyldigTom;

    @Column(name = "nom_dol")
    @Convert(converter = YesNoConverter.class)
    private Boolean nomDol;

    @Column(name = "nom_ot_sektor")
    @Enumerated(EnumType.STRING)
    private Sektor nomOtSektor;

    @Column(name = "nom_orgtilknytning")
    private String nomOrgtilknytning;
}
