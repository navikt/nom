package no.nav.nom.apps.api.nik;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.contract.nom.IdentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupNavidentResponse {
    private String navident;
    private IdentType identType;
}
