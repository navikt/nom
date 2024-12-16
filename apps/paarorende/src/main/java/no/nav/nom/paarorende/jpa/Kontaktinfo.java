package no.nav.nom.paarorende.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Kontaktinfo extends AuditingMetadata {

    @Id
    @SequenceGenerator(name = "idGenerator", sequenceName = "KONTAKTINFO_SEQ", allocationSize = 1)
    @GeneratedValue(generator = "idGenerator", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "KONTAKT_ID")
    private String kontakId;
    private String navident;
    private String fornavn;
    private String etternavn;

    @Enumerated(EnumType.STRING)
    private Relasjon relasjon;
    private String telefon;
    private Integer prioritet;
    @Embedded
    private Adresse adresse;

    private LocalDate datoSlettet;
}
