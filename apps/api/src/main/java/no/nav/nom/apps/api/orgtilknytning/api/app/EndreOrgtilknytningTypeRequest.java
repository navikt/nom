package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.contract.nom.OrgtilknytningType;

import java.util.Optional;

public record EndreOrgtilknytningTypeRequest(
    Optional<OrgtilknytningType> orgtilknytningType
) {
}
