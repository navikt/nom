package no.nav.nom.apps.api.graphql.pdlutils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NavnDto {
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
}
