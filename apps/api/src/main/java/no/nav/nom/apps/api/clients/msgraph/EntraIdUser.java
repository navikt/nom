package no.nav.nom.apps.api.clients.msgraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record EntraIdUser(

        @JsonProperty("mail")
        String email,

        @JsonProperty("onPremisesSamAccountName")
        String navIdent
) {
}
