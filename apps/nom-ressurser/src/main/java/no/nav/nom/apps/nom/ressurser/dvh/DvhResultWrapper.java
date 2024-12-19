package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

public class DvhResultWrapper {
    @JsonProperty("Nom_AnsatteResult")
    private List<DvhRessursTdvDTO> ansatteResultList;

    public List<DvhRessursTdvDTO> getAnsatteResultList() {
        return ansatteResultList;
    }
}


