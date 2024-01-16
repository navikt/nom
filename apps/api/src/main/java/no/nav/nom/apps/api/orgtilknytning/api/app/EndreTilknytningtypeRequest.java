package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.contract.nom.Tilknytningtype;

import java.util.Optional;

public record EndreTilknytningtypeRequest(
    Optional<Tilknytningtype> tilknytningtype
) {}
