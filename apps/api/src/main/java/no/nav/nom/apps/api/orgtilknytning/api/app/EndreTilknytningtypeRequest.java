package no.nav.nom.apps.api.orgtilknytning.api.app;

import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

@Builder(toBuilder = true)
public record EndreTilknytningtypeRequest(
    Tilknytningtype tilknytningtype
) {}
