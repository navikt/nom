package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HendelserState(
    String fid,
    String id,
    LocalDateTime timestamp,
    EntityType entityType,
    HendelserType hendelserType
) {
}
