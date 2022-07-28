package no.nav.nom.apps.navident.pdl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class PdlIdent {
    String ident;
    String gruppe;
    boolean historisk;

    public boolean erGjeldende() { return !historisk; }
    public boolean erFolkeregisterId() {
        return gruppe.equals("FOLKEREGISTERIDENT");
    }
    public boolean erGjeldendeFolkeregisterId() {
        return erGjeldende() && erFolkeregisterId();
    }
}
