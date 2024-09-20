package no.nav.nom.apps.api.orgtilknytning.api.email;

import lombok.Builder;

@Builder(toBuilder = true)
public record OTEndretData(
    String fid,
    String navident,
    OTEndretDataType eventType,
    String changedBy,
    String timestamp
) {

    public enum OTEndretDataType {
        OT_NY,
        OT_ENDRET,
        OT_FJERNET,
        OT_AVSLUTTET
    }
}
