package no.nav.nom.skjermede.personer.common.unntakskode;

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

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_UNNTAKSKODE")
public class Unntakskode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unntakskodeIdGenerator")
    @SequenceGenerator(name = "unntakskodeIdGenerator", sequenceName = "T_UNNTAKSKODE_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "KODE")
    private String kode;

    @NotNull
    @Column(name = "BESKRIVELSE")
    private String beskrivelse;

}
