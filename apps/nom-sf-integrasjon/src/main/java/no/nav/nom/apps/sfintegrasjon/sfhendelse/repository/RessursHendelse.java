package no.nav.nom.apps.sfintegrasjon.sfhendelse.repository;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "ressurs_hendelse")
@NoArgsConstructor
@AllArgsConstructor
public class RessursHendelse {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sporings_id")
    private String sporingsId;

    @Column(name = "hendelse_type", nullable = false)
    private String hendelseType;

    @Column(name = "mottattTidspunkt", nullable = false)
    private LocalDateTime mottattTidspunkt;

    @Column(name = "endret_av", nullable = false)
    private String endretAv;

    @Column(name = "navident", nullable = false)
    private String navident;

    @Type(JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    private String data;

    @Column(name = "behandlings_id")
    private Long behandlingsId;
}
