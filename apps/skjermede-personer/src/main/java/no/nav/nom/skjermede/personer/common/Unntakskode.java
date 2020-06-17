package no.nav.nom.skjermede.personer.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Unntakskode {
    KODE001("Pensjonistutbetaling");

    public final String beskrivelse;
}
