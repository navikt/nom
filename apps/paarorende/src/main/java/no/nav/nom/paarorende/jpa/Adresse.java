package no.nav.nom.paarorende.jpa;

import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Adresse {

    private String adresse;
    private String postnr;
    private String poststed;
    private String land;
}
