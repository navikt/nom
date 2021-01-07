package no.nav.nom.contract.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StillingTabell {
    public static final String TYPE = "no.nav.nom.dvh:stilling-tabell";

    @JsonProperty("stilling2nomResult")
    List<Stilling> elementer;

}
