package no.nav.nom.apps.api.orgenhet.implementations;

import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
public class OrgenhetService implements IOrgenhetService {
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository){
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    @Override
    public Optional<IOrgenhet> getById(String id) {
        return map(orgEnhetStateRepository.findById(id));
    }

    @Override
    public Optional<IOrgenhet> getByFid(String fid) {
        return map(orgEnhetStateRepository.findByFid(fid));
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return map(
                orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                .stream()
                .max(comparing(OrgEnhetState::gyldigFom))
            );
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }

    @Override
    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return map(orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .max(comparing(OrgEnhetState::gyldigFom))
        );
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds) {
        return orgEnhetStateRepository.findAllByAgressoIdIn(agressoIds).stream().map(this::map).toList();
    }

    @Override
    public List<IOrgenhet> findEnheterByAgressoIdConcatOrgnivIn(Set<IOrgenhet.AgressoIdCombo> agressoIdConcatOrgnivSet) {
        var agressoIdAndNivs =  agressoIdConcatOrgnivSet.stream()
            .map(it -> it.agressoId() + "--" + it.orgNiv())
            .collect(Collectors.toSet());
        return orgEnhetStateRepository.findByAgressoIdAndNivIn(agressoIdAndNivs).stream().map(this::map).toList();
    }

    @Override
    public Collection<IOrgenhet> getAllEnheter() {
        return orgEnhetStateRepository.getAll().stream().map(this::map).toList();
    }

    @Override
    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgEnhetStateRepository.getAll().stream()
            .filter( orgEnhetState -> orgEnhetState.agressoId().isPresent() && orgEnhetState.agressoId().get().contains(agressoId))
            .map(this::map)
            .toList();
    }

    @Override
    public List<IOrgenhet> matchByNavn(String navn) {
        return orgEnhetStateRepository.getAll().stream()
            .filter( orgEnhetState -> orgEnhetState.navn().toLowerCase().contains(navn))
            .map(this::map)
            .toList();
    }

    @Override
    public Map<String, IOrgenhet> getByIdBulk(List<String> ids) {
        return orgEnhetStateRepository.findByIdIn(ids).stream().collect(Collectors.toMap(OrgEnhetState::id, this::map));
    }

    @Override
    public Map<String, IOrgenhet> getByFidBulk(List<String> fids) {
        return orgEnhetStateRepository.findByIdIn(fids).stream().collect(Collectors.toMap(OrgEnhetState::id, this::map));
    }

    private Optional<IOrgenhet> map(Optional<OrgEnhetState> orgEnhetState) {
        return Optional.ofNullable(
            orgEnhetState.isEmpty() ?
                null :
                new OrgEnhet(orgEnhetState.get())
        );
    }

    private IOrgenhet map(OrgEnhetState orgEnhetState) {
        return new OrgEnhet(orgEnhetState);
    }
}
