package no.nav.nom.apps.api.nik;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpprettNavidentRequest {

    private String personident;
    private String forbokstav;
    private String identType;
}
