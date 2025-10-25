package no.nav.nom.apps.api.hierarki.initals;

import no.nav.nom.apps.api.hierarki.HierarkiService;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

public interface InitHierarkiInterface {
    void upsertInitHierarki(HierarkiService.Upserts upserts);

    default void upsertItemsInRandomOrder(List<Record> upsertStateObjectList, HierarkiService.Upserts upserts, long randomSeed) {

        RandomGenerator random = new Random(randomSeed);
        System.out.println("Random seed for upsert item shuffle is " + randomSeed);

        var upsertStateObjectListCopy = new ArrayList<Record>(upsertStateObjectList);

        Collections.shuffle(upsertStateObjectListCopy, random);

        for(var upsertObjectState : upsertStateObjectListCopy){
            switch(upsertObjectState){
                case OrgEnhetState oe -> upserts.upsertEnhet(oe);
                case RessursState rs -> upserts.upsertRessurs(rs);
                case OrgtilknytningState ot -> upserts.upsertOrgtilknytningState(ot);
                case LederTilgangState lts -> upserts.upsertLederTilgang(lts);
                case OrganiseringState os -> upserts.upsertOrganisering(os);
                case OrgEnhetLederState oels -> upserts.upsertLeder(oels);
                default -> {
                    throw new IllegalStateException("Not a valid class to upsert: " + upsertObjectState.getClass().getName());
                }
            }
        }
    }
}
