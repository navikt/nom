package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1;

import lombok.Builder;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record HendelserState(
    String id,
    LocalDateTime timestamp,
    EntityType entityType,
    HendelserType hendelserType
) {
}
