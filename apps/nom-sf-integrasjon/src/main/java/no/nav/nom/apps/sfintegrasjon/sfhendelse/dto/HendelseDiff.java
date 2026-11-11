package no.nav.nom.apps.sfintegrasjon.sfhendelse.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = NyRessurs.class, name = "NyRessurs"),
    @JsonSubTypes.Type(value = VisningsnavnEndring.class, name = "VisningsnavnEndring"),
    @JsonSubTypes.Type(value = OrgtilknytningerEndring.class, name = "OrgtilknytningerEndring")
})
public interface HendelseDiff {}
