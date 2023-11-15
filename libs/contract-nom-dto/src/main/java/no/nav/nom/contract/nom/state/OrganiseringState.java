package no.nav.nom.contract.nom.state;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record OrganiseringState(
    String fid,
    String id,
    String navn,
    String idOver,
    String navnOver,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom
) {}
