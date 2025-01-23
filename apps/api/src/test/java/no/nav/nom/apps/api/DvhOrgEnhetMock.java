package no.nav.nom.apps.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Data
@Builder
public class DvhOrgEnhetMock {
    @JsonProperty("orgenhet_kode")
    private String ORGENHET_KODE;
    @JsonProperty("orgenhet_navn")
    private String ORGENHET_NAVN;
    @JsonProperty("orgnode_niv")
    private String ORGNODE_NIV;
    @JsonProperty("enhettype_kode")
    private String ENHETTYPE_KODE;
    @JsonProperty("enhettype_navn")
    private String ENHETTYPE_NAVN;
    @JsonProperty("orgenhet_status")
    private String ORGENHET_STATUS;
    @JsonProperty("orgenhet_fra_dato")
    private int ORGENHET_FRA_PERIODE;
    @JsonProperty("orgenhet_til_dato")
    private int ORGENHET_TIL_PERIODE;
    @JsonProperty("stilling_maks_gyldig_til_dato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX")
    private OffsetDateTime STILLING_MAKS_GYLDIG_TIL_DATO;
    @JsonProperty("leder_ressursnr")
    private String LEDER_RESSURSNR;
    @JsonProperty("lastet_dato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX")
    private OffsetDateTime LASTET_DATO;
    @JsonProperty("orgenhet_type")
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
                .LASTET_DATO(getOffsetDateTime(2022,4,1))
                .STILLING_MAKS_GYLDIG_TIL_DATO(getOffsetDateTime(2099,12,31));
    }

    public static OffsetDateTime getOffsetDateTime(int year, int month, int day) {
        var ldt = LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIDNIGHT);
        var zdt = ZonedDateTime.of(ldt, ZoneOffset.systemDefault());
        return zdt.withZoneSameInstant(ZoneId.of("Z")).toOffsetDateTime();
    }
}