package no.nav.nom.libs.lucenewrapper.domain;

public record LuceneRessurs(String navident, String navn, String tlf, String epost, boolean active, String sektor) {

    public LuceneRessurs(String navident, String navn, String tlf, String epost){
        this(navident,navn,tlf,epost,true,"NAV_STATLIG");
    }

    public LuceneRessurs(String navident, String navn){
        this(navident,navn,"","",true,"NAV_STATLIG");
    }

    public LuceneRessurs(String navident, String navn, boolean active){
        this(navident,navn,"","",active,"NAV_STATLIG");
    }

}
