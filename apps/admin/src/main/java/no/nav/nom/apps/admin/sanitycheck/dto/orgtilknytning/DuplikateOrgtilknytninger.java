package no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

public record DuplikateOrgtilknytninger(
        OrgtilknytningState en,
        OrgtilknytningState to
) {
}
