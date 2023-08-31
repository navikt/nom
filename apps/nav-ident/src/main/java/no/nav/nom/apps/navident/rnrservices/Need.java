package no.nav.nom.apps.navident.rnrservices;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.IdentType;

import java.util.List;

@Builder
@Data
@Slf4j
public class Need implements Cloneable {

    String uuid;

    String personid;
    IdentType identType;
    char idPrefix;

    String eksisterendeId;
    boolean personOppslagVisited;

    String pdlGjeldendePersonid;
    List<String> pdlPersonider;
    boolean pdlOppslagVisited;

    String generertId;
    boolean idGeneratorVisited;

    Respons respons;
    boolean responsProdusentVisited;

    int visited;

    @Override
    public Need clone() {
        Need clone = null;
        try {
            clone = (Need)super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Could not clone myself - ", e);
            throw new RuntimeException("Could not clone myself - ", e);
        }
        clone.pdlPersonider = this.pdlPersonider != null ? List.copyOf(this.pdlPersonider) : null;
        return clone;
    }

    public String maskedToString() {
        Need masked = this.clone();
        masked.personid = mask(this.personid);
        masked.pdlPersonider = this.pdlPersonider != null ? this.pdlPersonider.stream().map(this::mask).toList() : null;
        masked.pdlGjeldendePersonid =  mask(this.pdlGjeldendePersonid);
        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
