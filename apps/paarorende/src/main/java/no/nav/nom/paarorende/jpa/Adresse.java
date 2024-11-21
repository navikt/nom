package no.nav.nom.paarorende.jpa;

import jakarta.persistence.Embeddable;

@Embeddable
public class Adresse {

    private String gateadresse;
    private String postnr;
    private String poststed;
    private String land;
}
