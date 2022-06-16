package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class Personid {
    String id;
    PersonidType type;

    @Override
    public Personid clone() {
        try {
            return (Personid) super.clone();
        } catch (CloneNotSupportedException e) {
            return Personid.builder().id(this.id).type(this.type).build();
        }
    }
}
