package no.nav.nom.apps.nom.enheter.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
class DvhResultWrapper {
    @JsonProperty("NOM_HRORG_HIEARKI_PARAMResult")
    private List<DvhEnhetDTO> enhetResultList;
}
