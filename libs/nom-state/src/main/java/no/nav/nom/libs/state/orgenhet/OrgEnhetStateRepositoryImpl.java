package no.nav.nom.libs.state.orgenhet;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
public class OrgEnhetStateRepositoryImpl implements OrgEnhetStateRepository {
    private final Map<String, OrgEnhetState> orgEnhetByFid = new ConcurrentHashMap<>();
    private final Map<String, OrgEnhetState> orgEnhetById = new ConcurrentHashMap<>();

    private final Map<String, List<OrgEnhetState>> orgEnheterByUniqueAgressoId = new ConcurrentHashMap<>();
    private final Map<String, List<OrgEnhetState>> orgEnheterByAgressoIdAndNiv = new ConcurrentHashMap<>();

    @Override
    public Optional<OrgEnhetState> findByFid(String fid) {
        return Optional.ofNullable(orgEnhetByFid.get(fid));
    }

    @Override
    public List<OrgEnhetState> findByFidIn(List<String> fids) {
        return orgEnhetByFid.entrySet().stream()
            .filter(e -> fids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<OrgEnhetState> findById(String id) {
        return Optional.ofNullable(orgEnhetById.get(id));
    }

    @Override
    public List<OrgEnhetState> findByIdIn(List<String> ids) {
        return orgEnhetById.entrySet().stream()
            .filter(e -> ids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgEnhetState> findByIdIn(Set<String> ids) {
        return orgEnhetById.entrySet().stream()
            .filter(e -> ids.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgEnhetState> findAllByUniqueAgressoId(String id) {
        List<OrgEnhetState> orgEnheter = orgEnheterByUniqueAgressoId.get(id);
        if (orgEnheter == null) {
            orgEnheter = new ArrayList();
            orgEnheterByUniqueAgressoId.put(id, orgEnheter);
        }
        return orgEnheter;
    }

    @Override
    public List<OrgEnhetState> findAllByAgressoIdIn(Set<String> agressoIds) {
        return getAll().stream().filter(orgEnhetState -> agressoIds.contains(orgEnhetState.agressoId().orElseGet(null))).toList();
    }

    @Override
    public List<OrgEnhetState> findAllByAgressoIdAndNiv(String id) {
        List<OrgEnhetState> orgEnheter = orgEnheterByAgressoIdAndNiv.get(id);
        if (orgEnheter == null) {
            orgEnheter = new ArrayList();
            orgEnheterByAgressoIdAndNiv.put(id, orgEnheter);
        }
        return orgEnheter;
    }

    @Override
    public Optional<OrgEnhetState> findNewestUniqueAgressoId(String id) {
        return findAllByUniqueAgressoId(id).stream()
            .filter(orgEnhetState -> !orgEnhetState.endedOnStartDate())
            .max(comparing(OrgEnhetState::gyldigFom));
    }

    @Override
    public Collection<OrgEnhetState> getAll() {
        return orgEnhetByFid.values();
    }

    @Override
    public int count() { return orgEnhetByFid.size();  }

    @Override
    public void update(OrgEnhetState newOrgEnhetState) {
        orgEnhetByFid.put(newOrgEnhetState.fid(), newOrgEnhetState);
        orgEnhetById.put(newOrgEnhetState.id(), newOrgEnhetState);
        addToUniqueAgressoIdList(newOrgEnhetState);
        addToAgressoIdAndNivList(newOrgEnhetState);
    }

    @Override
    public void clear() {
        orgEnhetByFid.clear();
        orgEnhetById.clear();
        orgEnheterByUniqueAgressoId.clear();
        orgEnheterByAgressoIdAndNiv.clear();
    }


    void addToUniqueAgressoIdList(OrgEnhetState newOrgEnhetState) {
        if (newOrgEnhetState.uniqueAgressoId().isPresent()) {
            List<OrgEnhetState> orgEnheter = findAllByUniqueAgressoId(newOrgEnhetState.uniqueAgressoId().get());
            var enhet = orgEnheter.stream().filter(orgEnhet -> orgEnhet.fid().equals(newOrgEnhetState.fid())).findAny();
            if (enhet.isPresent()) {
                orgEnheter.remove(enhet.get());
            }
            orgEnheter.add(newOrgEnhetState);
            orgEnheterByUniqueAgressoId.put(newOrgEnhetState.uniqueAgressoId().get(), orgEnheter);
        }
    }

    void addToAgressoIdAndNivList(OrgEnhetState newOrgEnhetState) {
        if (newOrgEnhetState.agressoId().isPresent() && newOrgEnhetState.agressoOrgNivaa().isPresent()) {
            var concatIdNiv = newOrgEnhetState.agressoId().get() + "--" + newOrgEnhetState.agressoOrgNivaa().get();
            List<OrgEnhetState> orgEnheter = findAllByAgressoIdAndNiv(concatIdNiv);
            var enhet = orgEnheter.stream().filter(orgEnhet -> orgEnhet.fid().equals(newOrgEnhetState.fid())).findAny();
            if (enhet.isPresent()) {
                orgEnheter.remove(enhet.get());
            }
            orgEnheter.add(newOrgEnhetState);
            orgEnheterByAgressoIdAndNiv.put(concatIdNiv, orgEnheter);
        }
    }
}
