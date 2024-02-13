package no.nav.nom.apps.api.orgtilknytning.api.app;

import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.util.Optional;

@Builder(toBuilder = true)
public record EndreTilknytningtypeRequest(
    Optional<Tilknytningtype> tilknytningtype
) {}
