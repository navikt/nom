package no.nav.nom.paarorende.jpa;

public enum Relasjon {
    EKTEFELLE("Ektefelle"),
    SAMBOER("Samboer"),
    PARTNER("Partner"),
    BARN("Barn"),
    SOSKEN("Søsken"),
    FORELDER("Forelder"),
    ANNET("Annet");

    private String relasjon;

    Relasjon(String relasjon) {
        this.relasjon = relasjon;
    }
}
