package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavnResponseDTO {

    private String fornavn;
    private String etternavn;
}
