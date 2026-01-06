package no.nav.nom.libs.gql.hierarchygeneration;

import no.nav.nom.contract.nom.state.OrgEnhetState;

public record NomId(String nomId) {
    public NomId{
        // todo, validation
    }
    public static NomId create(String oeId) {
        return new NomId(oeId);
    }

    public static NomId create(OrgEnhetState orgEnhetState) {
        return new NomId(orgEnhetState.id());
    }
}
