package no.nav.nom.apps.nom.enheter.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
class DvhResponseWrapper {
    @JsonProperty("NOM_HRORG_HIEARKI_PARAMResponse")
    private DvhResultWrapper dvhResultWrapper;


    List getEnhetResultList(){
        return (dvhResultWrapper != null) ? dvhResultWrapper.getEnhetResultList() : null;
    }
}
