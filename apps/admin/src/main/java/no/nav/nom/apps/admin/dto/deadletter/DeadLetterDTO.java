package no.nav.nom.apps.admin.dto.deadletter;

public record DeadLetterDTO(
    String fid,
    String header,
    String orgtilknytningState
) {
}
