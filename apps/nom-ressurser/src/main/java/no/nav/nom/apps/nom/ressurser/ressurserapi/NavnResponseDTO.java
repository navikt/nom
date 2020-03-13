package no.nav.nom.apps.nom.ressurser.ressurserapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavnResponseDTO {

    @JsonProperty
    private String fornavn;

    @JsonProperty
    private String etternavn;
}
