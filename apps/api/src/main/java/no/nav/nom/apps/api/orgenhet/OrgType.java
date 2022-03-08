package no.nav.nom.apps.api.orgenhet;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class OrgType {

    String kode;
    String navn;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

}
