package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
class DvhResponseWrapper {
    @JsonProperty("Nom_AnsatteResponse")
    private DvhResultWrapper dvhResultWrapper;

    List<DvhRessursTdvDTO> getAnsattResultList(){
        return (dvhResultWrapper != null) ? dvhResultWrapper.getAnsatteResultList() : null;
    }
}
