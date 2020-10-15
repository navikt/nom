package no.nav.nom.apps.nom.enheter.dvh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
}