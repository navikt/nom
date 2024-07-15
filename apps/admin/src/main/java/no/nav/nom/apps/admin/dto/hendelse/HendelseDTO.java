package no.nav.nom.apps.admin.dto.hendelse;

import lombok.Builder;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelseType;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record HendelseDTO(
    String id,
    LocalDateTime timestamp,
    EntityType entityType,
    HendelseType hendelseType
) {
}