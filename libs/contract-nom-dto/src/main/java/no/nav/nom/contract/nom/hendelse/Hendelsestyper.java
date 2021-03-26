package no.nav.nom.contract.nom.hendelse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Hendelsestyper {
    OPERASJON_V1("no.nav.nom.hendelser:Operasjon:v1"),
    RESSURS_V1("no.nav.nom.hendelser:Ressurs:v1"),
    ENHET_V1("no.nav.nom.hendelser:Enhet:v1");

    String key;
}
