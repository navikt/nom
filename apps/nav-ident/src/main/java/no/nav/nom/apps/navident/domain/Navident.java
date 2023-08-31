package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.IdentType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
public class Navident {

    String navident;
    String gjeldendePersonid;
    List<String> allePersonider;
    IdentType identType;

    public boolean addPersonid(String personid) {
        List<String> personider = new ArrayList<>(allePersonider);
        allePersonider = personider;
        return allePersonider.add(personid);
    }

    public boolean is(String personid) {
        return allePersonider.stream().anyMatch( id -> id.equals(personid));
    }

    @Override
    public Navident clone() {
        Navident clone = null;
        try {
            clone = (Navident) super.clone();
        } catch(CloneNotSupportedException e) {
            clone = Navident.builder()
                    .navident(this.navident)
                    .gjeldendePersonid(this.getGjeldendePersonid())
                    .identType(this.identType)
                    .build();
        }
        clone.allePersonider = new ArrayList<>(this.allePersonider);
        return clone;
    }

    public String maskedToString() {
        Navident masked = this.clone();
        masked.gjeldendePersonid = mask(this.gjeldendePersonid);
        masked.allePersonider =
                this.allePersonider.stream().map( p -> mask(p)).collect(Collectors.toList());
        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
