package no.nav.nom.apps.api.pdlutils;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.nom.apps.api.rest.dto.NavnDTO;

import java.util.List;

@Data
@AllArgsConstructor
public class PersonDto {
    private List<NavnDTO> navn;
    private List<KjonnDTO> kjoenn;
}
