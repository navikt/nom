package no.nav.nom.apps.api.pdlutils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PersonDto {
    private List<NavnDto> navn;
}
