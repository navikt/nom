package no.nav.nom.skjermede.personer.pip.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SkjermetDataRequestDTO {
    @JsonProperty(required = true)
    private String personident;
}
