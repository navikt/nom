package no.nav.nom.apps.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Data
@Builder
public class DvhOrgEnhetMock {
    @JsonProperty("ORGENHET_KODE")
    private String ORGENHET_KODE;
    @JsonProperty("ORGENHET_NAVN")
    private String ORGENHET_NAVN;
    @JsonProperty("ORGNODE_NIV")
    private String ORGNODE_NIV;
    @JsonProperty("ENHETTYPE_KODE")
    private String ENHETTYPE_KODE;
    @JsonProperty("ENHETTYPE_NAVN")
    private String ENHETTYPE_NAVN;
    @JsonProperty("ORGENHET_STATUS")
    private String ORGENHET_STATUS;
    @JsonProperty("ORGENHET_FRA_PERIODE")
    private int ORGENHET_FRA_PERIODE;
    @JsonProperty("ORGENHET_TIL_PERIODE")
    private int ORGENHET_TIL_PERIODE;
    @JsonProperty("STILLING_MAKS_GYLDIG_TIL_DATO")
    private LocalDate STILLING_MAKS_GYLDIG_TIL_DATO;
    @JsonProperty("LEDER_RESSURSNR")
    private String LEDER_RESSURSNR;
    @JsonProperty("LASTET_DATO")
    private LocalDate LASTET_DATO;
    @JsonProperty("ORGENHET_TYPE")
    private OrgEnhetsType ORGENHET_TYPE;

    public static DvhOrgEnhetMockBuilder getBuilderWithDefault() {
        return DvhOrgEnhetMock.builder()
                .ORGENHET_KODE("testKode")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_KODE("80")
                .ENHETTYPE_NAVN("ENHETTYPE")
                .ORGENHET_STATUS("N")
                .ORGENHET_FRA_PERIODE(202204)
                .ORGENHET_TIL_PERIODE(209912)
                .LASTET_DATO(LocalDate.parse("2022-04-01"))
                .STILLING_MAKS_GYLDIG_TIL_DATO(LocalDate.parse("2099-12-31"));
    }
}