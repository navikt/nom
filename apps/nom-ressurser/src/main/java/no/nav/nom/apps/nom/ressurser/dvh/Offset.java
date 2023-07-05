package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


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