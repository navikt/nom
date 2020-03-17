package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Table(name = "T_OFFSET")
@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Offset {

    @Id
    @Column(name = "OFFSET_NAME")
    private String offsetName;

    @Column(name = "OFFSET_VALUE")
    private Long offset;

    private final long incrementValue = 1000L;

    public Offset setOffset(long offset){
        this.offset = offset;
        return this;
    }

    public Long getIncrementedValue(){
        return offset+incrementValue;
    }
}