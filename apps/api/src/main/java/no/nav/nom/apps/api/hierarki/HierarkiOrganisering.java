package no.nav.nom.apps.api.hierarki;

import no.nav.nom.contract.nom.state.OrganiseringState;

public class HierarkiOrganisering {

    private final OrganiseringState backingState;

    HierarkiOrganisering(OrganiseringState organiseringState) {
        this.backingState = organiseringState;
    }
}
