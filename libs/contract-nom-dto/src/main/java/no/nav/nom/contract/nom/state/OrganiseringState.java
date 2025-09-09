package no.nav.nom.contract.nom.state;

import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OrganiseringState(
    String fid,
    String id,
    String navn,
    String idOver,
    String navnOver,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
    public boolean containsOrgEnhet(String id) {
        return this.id.equals(id) || this.idOver.equals(id);
    }
}
