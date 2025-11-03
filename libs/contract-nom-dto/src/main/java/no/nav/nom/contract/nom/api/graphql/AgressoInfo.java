package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgressoInfo {
    String agressoId;
    String agressoOrgNiv;
    String agressoOrgenhetType;
}
