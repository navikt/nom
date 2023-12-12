package no.nav.nom.apps.streams.kafkaapi.hendelser.v1;

import lombok.Builder;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelseType;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record Hendelse(
    String id,
    LocalDateTime timestamp,
    EntityType entityType,
    HendelseType hendelseType
) {
}
