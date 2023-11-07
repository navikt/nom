package no.nav.nom.contract.nom.state;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record OrganiseringState(
    String fid,
    String idOver,
    String idUnder,
    String navnOver,
    String navnUnder,
    Optional<LocalDate> gyldigTom,
    LocalDate gyldigFom
) {}
