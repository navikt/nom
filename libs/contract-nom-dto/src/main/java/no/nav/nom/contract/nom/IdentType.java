package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum IdentType {
    ROBOT, STANDARD, VIKAFOSSEN, UTENLANDSK_KONSULENT, UKJENT
}
