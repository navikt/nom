package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Tilknytningtype {
    ARBEIDSTRENING,
    ANNET,
    EKSTERN_SAMARBEIDSPARTNER,
    FAST,
    INGEN,
    JOBBSPESIALIST,
    KONSULENT,
    LAERLING,
    MIDLERTIDIG,
    PENSJONIST,
    STUDENT
}
