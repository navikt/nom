package no.nav.nom.apps.api.pdlutils;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.nom.apps.api.pdl.Kjonn;

import java.util.List;

@Data
@AllArgsConstructor
public class PersonDto {
    private List<NavnDto> navn;
    private List<KjonnDTO> kjoenn;
}
