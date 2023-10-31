package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

@Component
public class DvhOrganiseringMockUtils {


    public record DvhOrganiseringMock(
        @JsonProperty("ORGNODE_KODE")
        String ORGNODE_KODE,
        @JsonProperty("ORGNODE_NIVA")
        String ORGNODE_NIVA,
        @JsonProperty("agresso_orgnode_type")
        String agresso_orgnode_type,
        @JsonProperty("ORGNODE_NAVN")
        String ORGNODE_NAVN,
        @JsonProperty("LEDER_NAVIDENT")
        String LEDER_NAVIDENT,
        @JsonProperty("ORGNODE_KODE_OVER")
        String ORGNODE_KODE_OVER,
        @JsonProperty("ORGNODE_NIVA_OVER")
        String ORGNODE_NIVA_OVER,
        @JsonProperty("ORGNODE_NAVN_OVER")
        String ORGNODE_NAVN_OVER,
        @JsonProperty("ORGNODE_STATUS")
        String ORGNODE_STATUS
    ) {
        public static final DvhOrganiseringMock DEFAULT = new DvhOrganiseringMock(
            "2101556",
            "ORGENHET",
            "54",
            "Internasjonalt Bidrag",
            "Z123456",
            "210155",
            "ORGNIV3",
            "Internasjonalt Medlemskap og Bidrag",
            "C"
        );
    }

}
