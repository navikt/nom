package no.nav.nom.apps.api.orgtilknytning.api.email;

import lombok.Builder;

@Builder(toBuilder = true)
public record OTEndretData(
    String fid,
    String navident,
    String orgEnhetId,
    OTEndretDataType eventType,
    String changedByNavIdent,
    String timestamp
) {

    public enum OTEndretDataType {
        OT_NY("Ny organisasjonstilknytning opprettet"),
        OT_ENDRET("Annen endring"),
        OT_DATO_ENDRET("Gyldighetsperiode endret"),
        OT_FYSISK_BRUKERSTED_ENDRET("Endret fysisk brukersted"),
        OT_SEKTOR_ENDRET("Sektor endret"),
        OT_TILKNYTNINGTYPE_ENDRET("Tilknytningstype endret"),
        OT_DAGLIG_OPPFOLGING_ENDRET("Daglig oppfølging endret"),
        OT_FJERNET("Organisasjonstilknytning slettet"),
        OT_AVSLUTTET("Organisasjonstilknytning avsluttet");

        final String beskrivelse;
        OTEndretDataType(String beskrivelse) {
            this.beskrivelse = beskrivelse;
        }
    }
}
