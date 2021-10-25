package no.nav.nom.apps.api.graphql.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpprettRessursRequest {

    OrdreMetadata metadata;
    String personIdent;
    String visningsnavn;
}
