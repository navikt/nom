package no.nav.nom.apps.api.clients.msgraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ODataResponse<T>(

        @JsonProperty("@odata.context")
        String context,

        @JsonProperty("@odata.count")
        int count,

        @JsonProperty("@odata.nextLink")
        String nextLink,
        
        List<T> value

) {
}
