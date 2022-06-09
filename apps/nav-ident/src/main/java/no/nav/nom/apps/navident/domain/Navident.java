package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.LocalDate;
import java.util.List;

@Builder
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

}
