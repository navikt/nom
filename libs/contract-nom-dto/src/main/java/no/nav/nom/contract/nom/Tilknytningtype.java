package no.nav.nom.contract.nom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import static java.util.Objects.isNull;

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
    public static int compare(Tilknytningtype t1, Tilknytningtype t2) {
        if (isNull(t1)) {
            return 1;
        }

        if (isNull(t2)) {
            return -1;
        }

        if (t1.equals(t2)) {
            return 0;
        }

        if (t1.equals(Tilknytningtype.FAST)) {
            return -1;
        }

        if (t2.equals(Tilknytningtype.FAST)) {
            return 1;
        }

        return 0;
    }
}
