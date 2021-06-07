package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Kode {

    String kode;
    String navn;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
}
