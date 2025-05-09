package no.nav.nom.apps.admin.dto.deadletter;

import java.util.Map;

public record DeadLetterDTO(
    String fid,
    Map<String, String> headers,
    Object event
) {
}
