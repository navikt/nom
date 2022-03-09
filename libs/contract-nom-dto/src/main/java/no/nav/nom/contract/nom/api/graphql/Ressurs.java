package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class Ressurs {
    UUID nomId;
    String navIdent;
    String personIdent;
    List<Ressurstype> ressurstyper;
}

