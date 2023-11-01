package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DvhOrganiseringMock {

    @JsonProperty("ORGNODE_KODE")
    private final String ORGNODE_KODE;
    @JsonProperty("ORGNODE_NIVA")
    private final String ORGNODE_NIVA;
    @JsonProperty("agresso_orgnode_type")
    private final String agresso_orgnode_type;
    @JsonProperty("ORGNODE_NAVN")
    private final String ORGNODE_NAVN;
    @JsonProperty("LEDER_NAVIDENT")
    private final String LEDER_NAVIDENT;
    @JsonProperty("ORGNODE_KODE_OVER")
    private final String ORGNODE_KODE_OVER;
    @JsonProperty("ORGNODE_NIVA_OVER")
    private final String ORGNODE_NIVA_OVER;
    @JsonProperty("ORGNODE_NAVN_OVER")
    private final String ORGNODE_NAVN_OVER;
    @JsonProperty("ORGNODE_STATUS")
    private final String ORGNODE_STATUS;

    public static DvhOrganiseringMockBuilder getBuilderWithDefault() {
        return DvhOrganiseringMock.builder()
            .ORGNODE_KODE("2101556")
            .ORGNODE_NIVA("ORGENHET")
            .agresso_orgnode_type("54")
            .ORGNODE_NAVN("Internasjonalt Bidrag")
            .LEDER_NAVIDENT("Z123456")
            .ORGNODE_KODE_OVER("210155")
            .ORGNODE_NIVA_OVER("ORGNIV3")
            .ORGNODE_NAVN_OVER("Internasjonalt Medlemskap og Bidrag")
            .ORGNODE_STATUS("N");
    }

}
