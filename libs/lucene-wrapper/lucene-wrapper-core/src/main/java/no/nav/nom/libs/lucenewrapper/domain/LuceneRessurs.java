package no.nav.nom.libs.lucenewrapper.domain;

public record LuceneRessurs(String navident, String navn, String tlf, String epost, boolean active) {

    public LuceneRessurs(String navident, String navn, String tlf, String epost){
        this(navident,navn,tlf,epost,true);
    }

    public LuceneRessurs(String navident, String navn){
        this(navident,navn,"","",true);
    }

    public LuceneRessurs(String navident, String navn, boolean active){
        this(navident,navn,"","",active);
    }

}
