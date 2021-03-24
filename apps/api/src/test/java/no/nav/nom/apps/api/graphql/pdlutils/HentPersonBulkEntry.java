package no.nav.nom.apps.api.graphql.pdlutils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HentPersonBulkEntry {
    private String ident;
    private PersonDto person;
    private String code;
}
