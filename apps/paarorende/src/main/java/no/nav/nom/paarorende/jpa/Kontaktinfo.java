package no.nav.nom.paarorende.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.SequenceGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Kontaktinfo {

    @Id
    @SequenceGenerator(name = "idGenerator", sequenceName = "KONTAKTINFO_SEQ", allocationSize = 1)
    @GeneratedValue(generator = "idGenerator", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "KONTAKT_ID")
    private UUID kontakId;
    private String navident;
    private String fornavn;
    private String etternavn;

    @Enumerated(EnumType.STRING)
    private Relasjon relasjon;
    private String telefon;

    @Embedded
    private Adresse adresse;
    private LocalDate datoSlettet;
}
