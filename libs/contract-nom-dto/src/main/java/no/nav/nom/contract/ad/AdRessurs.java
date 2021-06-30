package no.nav.nom.contract.ad;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class AdRessurs {

    String navIdent;
    String email;
    String displayName;
    String givenName;
    String surname;
    @Default
    AdStatus status = AdStatus.OK;

    public static AdRessurs ikkeFunnet(String navIdent) {
        return AdRessurs.builder().navIdent(navIdent).status(AdStatus.IKKE_FUNNET).build();
    }

    public enum AdStatus {
        OK, IKKE_FUNNET
    }
}
