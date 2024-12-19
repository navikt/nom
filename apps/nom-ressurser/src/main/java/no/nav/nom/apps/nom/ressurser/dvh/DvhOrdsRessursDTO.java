package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.nom.ressurser.common.LocalDateDeserializer;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DvhOrdsRessursDTO implements DvhRessursDTO{
    @JsonProperty("pk_hrres_nom_historikk")
    private long offset;

    @JsonProperty("nav_id")
    private String navident;

    @JsonProperty("fnr")
    private String personIdent;

    @JsonProperty("fornavn")
    private String fornavn;

    @JsonProperty("etternavn")
    private String etternavn;

    @JsonProperty("epost")
    private String epost;

    @JsonProperty("remedy_start_dato")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate remedyStartDato;

    @JsonProperty("remedy_slutt_dato")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate remedySluttDato;

    @JsonProperty("remedy_permisjon")
    private String permisjon;

    @JsonProperty("agresso_slutt_dato")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate agressoSluttDato;

    @JsonProperty("ansatt_type")
    private String ansattType;

    @JsonProperty("ansatt_ressurs_kode")
    private String ressursTypeKode;
}