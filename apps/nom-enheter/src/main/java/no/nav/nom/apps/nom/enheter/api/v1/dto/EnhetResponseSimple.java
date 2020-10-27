package no.nav.nom.apps.nom.enheter.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnhetResponseSimple {
    private Long enhetsNr;

    private String navn;
}
