package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Tilknytningtype {
    ARBEIDSTRENING,
    ANNET,
    EKSTERN_SAMARBEIDSPARTNER,
    FAST,
    INGEN,
    KONSULENT,
    LAERLING,
    MIDLERTIDIG,
    PENSJONIST
}
