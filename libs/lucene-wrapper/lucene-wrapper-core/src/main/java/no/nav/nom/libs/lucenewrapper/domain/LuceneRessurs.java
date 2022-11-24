package no.nav.nom.libs.lucenewrapper.domain;

public record LuceneRessurs(String navident, String navn, String tlf, String epost) {

    public LuceneRessurs(String navident, String navn){
        this(navident,navn,"","");
    }
}
