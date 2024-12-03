package no.nav.nom.paarorende.jpa;

import jakarta.persistence.Column;

import jakarta.persistence.EntityListeners;

import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class AuditingMetadata {

    @CreatedBy
    @Column(name = "OPPRETTET_AV")
    public String opprettetAv;

    @CreatedDate
    @Column(name = "DATO_OPPRETTET")
    public LocalDate datoOpprettet;

    @LastModifiedBy
    @Column(name = "ENDRET_AV")
    public String endretAv;

    @LastModifiedDate
    @Column(name = "DATO_ENDRET")
    public LocalDate datoEndret;
}
