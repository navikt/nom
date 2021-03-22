package no.nav.nom.skjermede.personer.pip.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SkjermetDataBulkRequestDTO {
    @JsonSetter(contentNulls = Nulls.SKIP)
    private List<String> personidenter;
}
