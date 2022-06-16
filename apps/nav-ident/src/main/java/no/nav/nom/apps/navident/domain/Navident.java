package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

import java.awt.desktop.AppReopenedEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
public class Navident {

    String navident;
    String gjeldendePersonid;
    List<Personid> allePersonider;
    IdentType identType;
    List<Periode> aktivePerioder;

    public boolean addPeriode(Periode periode) {
        ArrayList<Periode> perioder = new ArrayList<>(aktivePerioder);
        aktivePerioder = perioder;
        return aktivePerioder.add(periode);
    }
    public boolean addPersonid(Personid personid) {
        ArrayList<Personid> personider = new ArrayList<>(allePersonider);
        allePersonider = personider;
        return allePersonider.add(personid);
    }

    public boolean is(String personid) {
        return allePersonider.stream().anyMatch( id -> id.personid.equals(personid));
    }

    public boolean isActive() { return isActive(LocalDate.now()); }

    public boolean isActive(LocalDate date) {
        return aktivePerioder.stream().anyMatch( periode -> periode.contains(date));
    }

    public LocalDate senestePeriodeDato() {
        Periode sistePeriode = aktivePerioder.get(aktivePerioder.size()-1);
        return sistePeriode.tilOgMed != null ? sistePeriode.tilOgMed : sistePeriode.fraOgMed;
    }

    public Navident clone() {
        Navident clone = Navident.builder()
                .navident(this.navident)
                .gjeldendePersonid(mask(this.gjeldendePersonid))
                .identType(this.identType)
                .build();
        clone.aktivePerioder = List.copyOf(this.aktivePerioder);
        clone.allePersonider = List.copyOf(this.allePersonider);
        return clone;
    }

    public String maskedToString() {
        Navident masked = this.clone();
        masked.gjeldendePersonid = mask(this.gjeldendePersonid);
        masked.allePersonider =
                this.allePersonider.stream().map( p ->
                        Personid.builder()
                                .personid(mask(p.personid))
                                .type(p.type)
                                .build()
                ).collect(Collectors.toList());
        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
