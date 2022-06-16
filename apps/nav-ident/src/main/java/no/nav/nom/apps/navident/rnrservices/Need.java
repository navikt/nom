package no.nav.nom.apps.navident.rnrservices;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.apps.navident.domain.IdentType;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
public class Need {
    String personid;
    IdentType identType;
    List<String> pdlPersonider;

    @Override
    public Need clone() {
        Need clone;
        try {
            clone = (Need)super.clone();
        } catch (CloneNotSupportedException e) {
            clone = Need.builder()
                    .personid(this.personid)
                    .identType(this.identType)
                    .build();
        }
        clone.pdlPersonider = this.pdlPersonider != null ? List.copyOf(this.pdlPersonider) : null;
        return clone;
    }

    public String maskedToString() {
        Need masked = this.clone();
        masked.personid = mask(this.personid);
        masked.pdlPersonider = this.pdlPersonider != null ? this.pdlPersonider.stream().map( p -> mask(p)).collect(Collectors.toList()) : null;
        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
