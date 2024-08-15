package no.nav.nom.apps.admin.dto.ressurs;

import java.time.LocalDateTime;

public record MasteredRessursDTO(String navident, LocalDateTime opprettet) {
    public static MasteredRessursDTO from(String navident, LocalDateTime opprettet) {
        return new MasteredRessursDTO(navident, opprettet);
    }
}
