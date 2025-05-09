package no.nav.nom.apps.admin.dto.deadletter;

import java.time.LocalDateTime;
import java.util.Map;

public record DeadLetterDTO(
    String fid,
    LocalDateTime timestamp,
    Map<String, String> headers,
    Object event
) {
}
