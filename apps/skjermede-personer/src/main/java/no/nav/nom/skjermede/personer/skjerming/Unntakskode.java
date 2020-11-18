package no.nav.nom.skjermede.personer.skjerming;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Unntakskode {
    KODE001("Tidligere ansatt"),
    KODE002("Tett knytning til NAV");

    public final String beskrivelse;
}
