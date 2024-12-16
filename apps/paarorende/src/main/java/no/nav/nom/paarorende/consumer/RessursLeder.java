package no.nav.nom.paarorende.consumer;

public record RessursLeder(Leder leder) {

    public String getNavident() {
        return leder.navident;
    }

    private record Leder (String navident) {}
}
