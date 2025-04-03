package no.nav.nom.contract.nom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@Schema(enumAsRef = true)
public enum Tilknytningtype {
    ANNET,
    ARBEIDSTRENING,
    EKSTERN_SAMARBEIDSPARTNER,
    FAST,
    INGEN,
    JOBBSPESIALIST,
    KONSULENT,
    LAERLING,
    MIDLERTIDIG,
    PENSJONIST,
    PRAKSISARBEID,
    STUDENT,
    UFORUTSETT_BEHOV,
    UTDANNINGSSTILLINGER,
    VIKAR;

    @JsonIgnore
    public static int compare(Optional<Tilknytningtype> t1, Optional<Tilknytningtype> t2) {
        if (t1.equals(t2)) {
            return 0;
        }

        if (t1.equals(Optional.of(Tilknytningtype.FAST))) {
            return -1;
        }

        if (t2.equals((Optional.of(Tilknytningtype.FAST)))) {
            return 1;
        }

        if (t1.equals(Optional.empty())) {
            return 1;
        }

        if (t2.equals(Optional.empty())) {
            return -1;
        }

        return 0;
    }
}
