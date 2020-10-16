package no.nav.nom.apps.nom.enheter.enhet;

import com.sun.istack.NotNull;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Entity
@Table(name = "ORGANISERING")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organisering {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organiseringIdGenerator")
    @SequenceGenerator(name = "organiseringIdGenerator", sequenceName = "T_ORGANISERING_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "OVERENHET")
    private Long overenhet;

    @Column(name = "UNDERENHET")
    private Long underenhet;

    @Column(name = "ORGANISERINGSTYPE")
    private String organiseringstype;

    @NotNull
    @Column(name = "GYLDIG_FRA")
    private LocalDate gyldigFra;

    @Column(name = "GYLDIG_TIL")
    private LocalDate gyldigTil;

    @NotNull
    @Column(name = "OPPRETTET_AV")
    private String opprettetAv;

    @NotNull
    @Column(name = "OPPRETT_DATO")
    private LocalDateTime opprettDato;

    @NotNull
    @Column(name = "ENDRET_AV")
    private String endretAv;

    @NotNull
    @Column(name = "ENDRET_DATO")
    private LocalDateTime endretDato;

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }
        else if(o instanceof Organisering){
            if(Objects.equals(overenhet, ((Organisering) o).getOverenhet()))
                if(Objects.equals(underenhet, ((Organisering) o).getUnderenhet()))
                    return Objects.equals(organiseringstype, ((Organisering) o).getOrganiseringstype());
        }
        return false;
    }
}
