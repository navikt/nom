package no.nav.nom.paarorende.api;

import lombok.Builder;
import java.util.List;

@Builder
public record RessursKontaktinfoDTO(
    String ressursNavident,
    List<KontaktinfoDTO> kontaktinfo
) {
}
