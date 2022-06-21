package no.nav.nom.apps.navident.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
        return allePersonider.stream().anyMatch( id -> id.id.equals(personid));
    }

    public boolean isActive() { return isActive(LocalDate.now()); }

    public boolean isActive(LocalDate date) {
        return aktivePerioder.stream().anyMatch( periode -> periode.contains(date));
    }

    public Periode senestePeriode() {
        return aktivePerioder.stream().sorted( (p1, p2) -> p2.fraOgMed.compareTo(p1.fraOgMed)).findFirst().get();
    }

    public LocalDate senestePeriodeDato() {
        Periode sistePeriode = senestePeriode();
        return sistePeriode.tilOgMed != null ? sistePeriode.tilOgMed : sistePeriode.fraOgMed;
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
        clone.aktivePerioder = this.aktivePerioder.stream().map(periode -> periode.clone()).collect(Collectors.toList());
        clone.allePersonider = this.allePersonider.stream().map(personid -> personid.clone()).collect(Collectors.toList());
        return clone;
    }

    public String maskedToString() {
        Navident masked = (Navident) this.clone();
        masked.gjeldendePersonid = mask(this.gjeldendePersonid);
        masked.allePersonider =
                this.allePersonider.stream().map( p ->
                        Personid.builder()
                                .id(mask(p.id))
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
