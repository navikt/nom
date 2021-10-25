package no.nav.nom.apps.api.graphql.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.api.graphql.request.dto.OrdreLinje;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ordre {

    UUID ordreId;
    List<OrdreLinje> ordreLinjer;
    String beskrivelse;

    UUID godkjentAv;
    LocalDateTime godkjentTidspunkt;
    UUID endretAv;
    LocalDateTime endretTidspunkt;

}
