package no.nav.nom.contract.nom.state;

import java.time.LocalDate;
import java.util.Optional;

public record OrgtilknytningState(
    String fid,
    String navident,
    String orgEnhetId,
    boolean erPrimaer,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom
) {}
