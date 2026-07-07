package no.nav.nom.apps.sfintegrasjon.nikclient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OpprettNavidentResponse {

    private String navident;
    private String feilmelding;
}
