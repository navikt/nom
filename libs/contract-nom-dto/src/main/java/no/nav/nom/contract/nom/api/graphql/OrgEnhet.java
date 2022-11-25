package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.time.LocalDate;

@Value
@Builder
public class OrgEnhet {
    String id;
    String agressoId;
    String orgNiv;
    String agressoOrgenhetType;
    String navn;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
    Kode type;
    OrgEnhetsKategori orgEnhetsKategori;
}
