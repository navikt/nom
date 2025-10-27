package no.nav.nom.apps.api.hierarki;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class HierarkiService {

    private final Map<NavIdent, Medarbeider> medarbeiderMap;
    private final Map<NomId, HierarkiOrgenhet> hierarkiOrgenhetMap;

    // setter til `null` for å slippe å vedlikeholde inntil videre
    private final Map<String, HierarkiOrgtilknytning> orgtilknytningMap = null;

    private final Map<NomId, PendingHierarkiOrgenhet> pendingHierarkiOrgEnhetMap = new HashMap<>();
    private final Map<NavIdent, PendingMedarbeider> pendingMedarbeiderMap = new HashMap<>();

    private final MapAccess mapAccess;

    public HierarkiService(Upserts eventListener) {
        // todo concurrent hashmap?
        this.hierarkiOrgenhetMap = new HashMap<>();
        this.medarbeiderMap = new HashMap<>();

        this.mapAccess = new MapAccess(this);

        eventListener.registerEventListener(this);
    }

    public Optional<Medarbeider> medarbeiderByNavident(NavIdent navident) {
        return Optional.ofNullable(this.medarbeiderMap.get(navident));
    }

    public Optional<HierarkiOrgenhet> enhetByNomId(NomId nomId) {
        return Optional.ofNullable(this.hierarkiOrgenhetMap.get(nomId));
    }

    private PendingHierarkiOrgenhet getOrCreatePendingOrgenhet(NomId nomId) {
        var maybeOut = this.pendingHierarkiOrgEnhetMap.get(nomId);
        if(maybeOut == null){
            var out = new PendingHierarkiOrgenhet();
            this.pendingHierarkiOrgEnhetMap.put(nomId, out);
            return out;
        }
        return maybeOut;
    }

    private PendingMedarbeider getOrCreatePendingMedarbeider(NavIdent navIdent) {
        var maybeOut = this.pendingMedarbeiderMap.get(navIdent);
        if(maybeOut == null){
            var out = new PendingMedarbeider();
            this.pendingMedarbeiderMap.put(navIdent, out);
            return out;
        }
        return maybeOut;
    }


    /**
     * Convenience class to give some access to the internal maps to other classes in the same package
     */
    static class MapAccess{
        private final HierarkiService hierarkiService;


        private MapAccess(HierarkiService hierarkiService) {
            this.hierarkiService = hierarkiService;
        }

        public Map<NavIdent, Medarbeider> getMedarbeiderMap(){
            return this.hierarkiService.medarbeiderMap;
        }

        public Map<NomId, HierarkiOrgenhet> getOrgenhetMap(){
            return this.hierarkiService.hierarkiOrgenhetMap;
        }

        public Map<String, HierarkiOrgtilknytning> getOrgtilknytningMap(){
            return this.hierarkiService.orgtilknytningMap;
        }
    }

    /**
     * To more easily customize how upsert events are triggered
     */
    public abstract static class Upserts {

        private HierarkiService hierarkiService = null;

        public Upserts() {}

        private void assertServiceIsRegistered(){
            if(null == this.hierarkiService){
                throw new IllegalStateException("Cannot upsert when HierarkiService == null. Ensure correct registration.");
            }
        }

        protected void registerEventListener(HierarkiService hierarkiService){
            if(this.hierarkiService != null){
                throw new RuntimeException("Already registered");
            }
            this.hierarkiService = hierarkiService;
        }


        // TODO for all the upsert methods, ensure state is correctly updated, since some states affect each other

        public void upsertRessurs(RessursState ressursState) {
            assertServiceIsRegistered();
            var navIdent = NavIdent.create(ressursState.navident());

            var maybePrevious = hierarkiService.medarbeiderMap.get(navIdent);
            if(maybePrevious == null){
                var newMedarbeider = new Medarbeider(ressursState, hierarkiService.mapAccess);
                hierarkiService.medarbeiderMap.put(navIdent,newMedarbeider);
                { // handle pending additional information
                    var pendingInfo = this.hierarkiService.pendingMedarbeiderMap.get(navIdent);
                    if (pendingInfo != null) {
                        pendingInfo.orgtilknytningStateList.forEach(newMedarbeider::updateOrgtilknytning);
                        pendingInfo.lederStateList.forEach(newMedarbeider::updateLeder);
                        this.hierarkiService.pendingMedarbeiderMap.remove(navIdent);
                    }
                }
            }else{
                maybePrevious.updateBackingState(ressursState);
            }
        }

        public void upsertEnhet(OrgEnhetState s) {
            assertServiceIsRegistered();
            var nomId = NomId.create(s);

            var maybePreviousOrgenhet = this.hierarkiService.hierarkiOrgenhetMap.get(nomId);
            if(maybePreviousOrgenhet == null){
                var orgenhet = new HierarkiOrgenhet(s,hierarkiService.mapAccess);
                hierarkiService.hierarkiOrgenhetMap.put(nomId, orgenhet);
                {//handle pending additional information
                    var pendingInfo = this.hierarkiService.pendingHierarkiOrgEnhetMap.get(nomId);
                    if (pendingInfo != null) {
                        pendingInfo.orgtilknytningStateList.forEach(orgenhet::updateOrgtilknytning);
                        pendingInfo.lederStateList.forEach(orgenhet::updateLeder);
                        pendingInfo.organiseringStateList.forEach(orgenhet::updateOrganisering);
                        this.hierarkiService.pendingHierarkiOrgEnhetMap.remove(nomId);
                    }
                }
            }else{
                maybePreviousOrgenhet.updateBackingState(s);
            }
        }

        public void upsertOrganisering(OrganiseringState s) {
            assertServiceIsRegistered();

            var affectedOrgenhetOverId = NomId.create(s.idOver());
            var affectedOrgenhetUnderId = NomId.create(s.id());
            var affectedOrgenhetOver = this.hierarkiService.hierarkiOrgenhetMap.get(affectedOrgenhetOverId);
            var affectedOrgenhetUnder = this.hierarkiService.hierarkiOrgenhetMap.get(affectedOrgenhetUnderId);

            if (affectedOrgenhetOver == null) {
                var pendingInfoOrgenhetOver = this.hierarkiService.getOrCreatePendingOrgenhet(affectedOrgenhetOverId);
                pendingInfoOrgenhetOver.organiseringStateList.add(s);
            } else {
                affectedOrgenhetOver.updateOrganisering(s);
            }

            if (affectedOrgenhetUnder == null) {
                var pendingInfoOrgenhetUnder = this.hierarkiService.getOrCreatePendingOrgenhet(affectedOrgenhetUnderId);
                pendingInfoOrgenhetUnder.organiseringStateList.add(s);

            } else {
                affectedOrgenhetUnder.updateOrganisering(s);
            }

        }

        public void upsertLederTilgang(LederTilgangState s) {
            assertServiceIsRegistered();
            throw new RuntimeException("Not implemented");
        }

        public void upsertLeder(OrgEnhetLederState orgEnhetLederState) {
            assertServiceIsRegistered();

            var affectedNomId = NomId.create(orgEnhetLederState.orgEnhetId());
            var affectedOrgenhet = this.hierarkiService.hierarkiOrgenhetMap.get(affectedNomId);
            var affectedNavIdent = NavIdent.create(orgEnhetLederState.navident());
            var affectedMedarbeider = this.hierarkiService.medarbeiderMap.get(affectedNavIdent);

            if(affectedOrgenhet == null){
                var pendingInfoOrgenhet = this.hierarkiService.getOrCreatePendingOrgenhet(affectedNomId);
                pendingInfoOrgenhet.lederStateList.add(orgEnhetLederState);
            }else{
                affectedOrgenhet.updateLeder(orgEnhetLederState);
            }

            if(affectedMedarbeider == null){
                var pendingInfoMedarbeider = this.hierarkiService.getOrCreatePendingMedarbeider(affectedNavIdent);
                pendingInfoMedarbeider.lederStateList.add(orgEnhetLederState);
            }else{
                affectedMedarbeider.updateLeder(orgEnhetLederState);
            }
        }

        public void upsertOrgtilknytningState(OrgtilknytningState s) {
            assertServiceIsRegistered();

            var affectedNomId = NomId.create(s.orgEnhetId());
            var affectedOrgenhet = this.hierarkiService.hierarkiOrgenhetMap.get(affectedNomId);
            var affectedNavIdent = NavIdent.create(s.navident());
            var affectedMedarbeider = this.hierarkiService.medarbeiderMap.get(affectedNavIdent);

            if(affectedOrgenhet == null ){
                var pendingInfoOrgenhet = this.hierarkiService.getOrCreatePendingOrgenhet(affectedNomId);
                pendingInfoOrgenhet.orgtilknytningStateList.add(s);
            }else{
                affectedOrgenhet.updateOrgtilknytning(s);
            }

            if (affectedMedarbeider == null) {
                var pendingInfoMedarbeider = this.hierarkiService.getOrCreatePendingMedarbeider(affectedNavIdent);
                pendingInfoMedarbeider.orgtilknytningStateList.add(s);
            } else {
                affectedMedarbeider.updateOrgtilknytning(s);
            }
//            this.hierarkiService.orgtilknytningMap.put(s.fid(), new HierarkiOrgtilknytning(s, this.hierarkiService.mapAccess));
        }

        public void upsertStateObject(Object stateObject){
            switch(stateObject){
                case OrgEnhetState oe -> this.upsertEnhet(oe);
                case RessursState rs -> this.upsertRessurs(rs);
                case OrgtilknytningState ot -> this.upsertOrgtilknytningState(ot);
                case LederTilgangState lts -> this.upsertLederTilgang(lts);
                case OrganiseringState os -> this.upsertOrganisering(os);
                case OrgEnhetLederState oels -> this.upsertLeder(oels);
                default -> {
                    throw new IllegalStateException("Not a valid class to upsert: " + stateObject.getClass().getName());
                }
            }
        }
    }

    private static final class PendingHierarkiOrgenhet {
        List<OrgEnhetLederState> lederStateList = new ArrayList<>(1);
        List<OrgtilknytningState> orgtilknytningStateList = new ArrayList<>(1);
        List<OrganiseringState> organiseringStateList = new ArrayList<>(1);

    }

    private static final class PendingMedarbeider {
        List<OrgEnhetLederState> lederStateList = new ArrayList<>(1);
        List<OrgtilknytningState> orgtilknytningStateList = new ArrayList<>(1);
    }
}
