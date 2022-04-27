package no.nav.nom.apps.api.hendelse.contract;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HendelseDataType {
    OPERASJON_V1("no.nav.nom.hendelser:Operasjon:v1"),
    RESSURS_V1("no.nav.nom.hendelser:Ressurs:v1"),
    ORGENHET_V1("no.nav.nom.hendelser:Organisasjonsenhet:v1");

    String dataType;

    HendelseDataType(String dataType){
        this.dataType = dataType;
    }

    @JsonValue
    public String getDataType(){
        return dataType;
    }
}
