package no.nav.nom.contract.nom.hendelse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Hendelsestyper {
    OPERASJON_V1("no.nav.nom.hendelser:Operasjon:v1"),
    RESSURS_V1("no.nav.nom.hendelser:Ressurs:v1"),
    ORGENHET_V1("no.nav.nom.hendelser:Organisasjonsenhet:v1");

    String key;
}
