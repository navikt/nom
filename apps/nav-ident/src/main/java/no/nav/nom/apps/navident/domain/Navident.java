package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Data
public class Navident {

    String navident;
    String gjeldendePersonid;
    @Singular("personid") List<Personid> allePersonider;
    IdentType identType;
    @Singular("periode") List<Periode> aktivePerioder;

    public boolean is(String personid) {
        return allePersonider.stream().anyMatch( id -> id.personid.equals(personid));
    }

    public boolean isActive() { return isActive(LocalDate.now()); }

    public boolean isActive(LocalDate date) {
        return aktivePerioder.stream().anyMatch( periode -> periode.contains(date));
    }

    public String maskedToString() {
        Navident masked = this.toBuilder().build(); // Is not a deep copy
        masked.gjeldendePersonid = mask(gjeldendePersonid);
        masked.allePersonider = new ArrayList<>();
        this.allePersonider.forEach( p ->
                masked.allePersonider.add(
                        Personid.builder()
                                .personid(mask(p.personid))
                                .type(p.type)
                                .build()
                )
        );

        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
