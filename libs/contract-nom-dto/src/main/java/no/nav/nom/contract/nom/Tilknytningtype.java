package no.nav.nom.contract.nom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Tilknytningtype {
    ARBEIDSTRENING,
    EKSTERN_SAMARBEIDSPARTNER,
    FAST,
    INGEN,
    KONSULENT,
    LAERLING,
    MIDLERTIDIG,
    @JsonIgnore MIDLERTIDLIG, // Vi tar inn denne igjen for å gjennomføre deprekrering av data i State først
    PENSJONIST
}
