package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Personid {
    String personid;
    PersonidType type;
}
