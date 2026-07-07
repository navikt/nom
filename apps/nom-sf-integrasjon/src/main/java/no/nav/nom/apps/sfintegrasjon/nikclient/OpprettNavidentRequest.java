package no.nav.nom.apps.sfintegrasjon.nikclient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpprettNavidentRequest {

    private String personid;
    private String identPrefix;
    private String identType;
}
