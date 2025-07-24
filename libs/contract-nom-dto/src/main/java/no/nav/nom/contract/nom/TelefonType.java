package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum TelefonType {
    NAV_TJENESTE_TELEFON,
    NAV_KONTOR_TELEFON,
    PRIVAT_TELEFON
}
