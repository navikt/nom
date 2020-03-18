package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class DvhResultWrapper {
    @JsonProperty("NOM_ANSATTEResult")
    private List<DvhRessursDTO> ansatteResultList;
}



