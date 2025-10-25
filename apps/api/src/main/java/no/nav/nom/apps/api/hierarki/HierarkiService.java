package no.nav.nom.apps.api.hierarki;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class HierarkiService {

    private final Map<NavIdent, Medarbeider> medarbeiderMap;
    private final Map<NomId, HierarkiOrgenhet> hierarkiOrgenhetMap;
    private final Map<String, HierarkiOrgtilknytning> orgtilknytningMap;

    private final List<DelayedStateObject> delayedStateObjectList = new ArrayList<>();

    private final MapAccess mapAccess;

    public HierarkiService(Upserts eventListener) {
        // todo concurrent hashmap?
        this.hierarkiOrgenhetMap = new HashMap<>();
        this.medarbeiderMap = new HashMap<>();
        this.orgtilknytningMap = new HashMap<>();

        this.mapAccess = new MapAccess(this);

        eventListener.registerEventListener(this);
    }

    public Optional<Medarbeider> medarbeiderByNavident(NavIdent navident) {
        return Optional.ofNullable(this.medarbeiderMap.get(navident));
    }

    public Optional<HierarkiOrgenhet> enhetByNomId(NomId nomId) {
        return Optional.ofNullable(this.hierarkiOrgenhetMap.get(nomId));
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

        private void handleDelayedUpsert(Collection<Object> dependencies){
            var currentDelayedStateQueueSize = this.hierarkiService.delayedStateObjectList.size();
            if(currentDelayedStateQueueSize > 150){
                // System.err.println("Delayed state object list has grown very large: " + currentDelayedStateQueueSize);
                log.warn("Delayed state object list has grown very large: " + currentDelayedStateQueueSize);
            }
            var delayedStateObjectsIterator = this.hierarkiService.delayedStateObjectList.iterator();
            while(delayedStateObjectsIterator.hasNext()){
                var delayedStateObject = delayedStateObjectsIterator.next();
                var readyToHandle = delayedStateObject.updateDependencies(dependencies);
                if(readyToHandle){
                    this.upsertStateObject(delayedStateObject.stateObject);
                    delayedStateObjectsIterator.remove();
                }
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
            hierarkiService.medarbeiderMap.put(NavIdent.create(ressursState.navident()),new Medarbeider(ressursState, hierarkiService.mapAccess));
            handleDelayedUpsert(List.of(NavIdent.create(ressursState.navident())));
        }

        public void upsertEnhet(OrgEnhetState s) {
            assertServiceIsRegistered();
            hierarkiService.hierarkiOrgenhetMap.put(NomId.create(s.id()), new HierarkiOrgenhet(s,hierarkiService.mapAccess));
            handleDelayedUpsert(List.of(NomId.create(s.id())));
        }

        public void upsertOrganisering(OrganiseringState s) {
            assertServiceIsRegistered();
            throw new RuntimeException("Not implemented");
        }

        public void upsertLederTilgang(LederTilgangState s) {
            assertServiceIsRegistered();
            throw new RuntimeException("Not implemented");
        }

        public void upsertLeder(OrgEnhetLederState s) {
            assertServiceIsRegistered();

            var affectedNomId = NomId.create(s.orgEnhetId());
            var affectedOrgenhet = this.hierarkiService.hierarkiOrgenhetMap.get(affectedNomId);
            var affectedNavIdent = NavIdent.create(s.navident());
            var affectedMedarbeider = this.hierarkiService.medarbeiderMap.get(affectedNavIdent);

            if(affectedOrgenhet == null || affectedMedarbeider == null){
                var dependencies = new ArrayList<>(2);
                if(affectedOrgenhet == null){
                    dependencies.add(affectedNomId);
                }
                if(affectedMedarbeider == null){
                    dependencies.add(affectedNavIdent);
                }
                this.hierarkiService.delayedStateObjectList.add(new DelayedStateObject(s,dependencies));
            } else {
                affectedOrgenhet.updateLeder(this.hierarkiService.medarbeiderByNavident(NavIdent.create(s.navident())).get());
            }
        }

        public void upsertOrgtilknytningState(OrgtilknytningState s) {
            assertServiceIsRegistered();

            var affectedNomId = NomId.create(s.orgEnhetId());
            var affectedOrgenhet = this.hierarkiService.hierarkiOrgenhetMap.get(affectedNomId);
            var affectedNavIdent = NavIdent.create(s.navident());
            var affectedMedarbeider = this.hierarkiService.medarbeiderMap.get(affectedNavIdent);

            if(affectedOrgenhet == null || affectedMedarbeider == null){
                var dependencies = new ArrayList<>(2);
                if(affectedOrgenhet == null){
                    dependencies.add(affectedNomId);
                }
                if(affectedMedarbeider == null){
                    dependencies.add(affectedNavIdent);
                }
                this.hierarkiService.delayedStateObjectList.add(new DelayedStateObject(s,dependencies));
            }else{
                affectedOrgenhet.updateOrgtilknytning(s);
                affectedMedarbeider.updateOrgtilknytning(s);
            }
            this.hierarkiService.orgtilknytningMap.put(s.fid(), new HierarkiOrgtilknytning(s, this.hierarkiService.mapAccess));
        }

        private void upsertStateObject(Object stateObject){
            // todo, handle more variants
            if (Objects.requireNonNull(stateObject) instanceof OrgtilknytningState ots) {
                this.upsertOrgtilknytningState(ots);
            } else if(Objects.requireNonNull(stateObject) instanceof OrgEnhetLederState orgEnhetLederState)
                this.upsertLeder(orgEnhetLederState);
            else {
                throw new IllegalArgumentException("Unknown state object: " + stateObject.getClass().getName());
            }
        }

    }

    /**
     * Some upserts have dependencies. If these dependencies are missing,
     * then this class is used to store them for later processing
     * when dependencies are available
     */
    private static class DelayedStateObject{
        final Object stateObject;
        private final List<Object> dependencies;

        private DelayedStateObject(Object stateObject, List<Object> dependencies){
            switch(stateObject){
                case RessursState rs -> {}
                case OrgEnhetState rs -> {}
                case LederTilgangState lts -> {}
                case OrganiseringState orgs -> {}
                case OrgtilknytningState orgt -> {}
                case OrgEnhetLederState loe -> {}
                default -> throw new RuntimeException("Unrecognized state object " +  stateObject.getClass().getName());
            }

            for(var dep : dependencies){
                switch(dep){
                    case NavIdent ignored -> {}
                    case NomId ignored -> {}
                    default -> throw new RuntimeException("Unrecognized dependency");
                }
            }

            this.stateObject = stateObject;
            this.dependencies = dependencies;
        }

        // returns true when all dependencies are available for state object to be processed
        boolean updateDependencies(Collection<Object> dependency){
            this.dependencies.removeAll(dependency);
            return this.dependencies.isEmpty();
        }
    }
}
