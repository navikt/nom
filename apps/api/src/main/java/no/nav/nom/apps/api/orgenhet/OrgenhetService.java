package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

@Service
public class OrgenhetService {
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private List<IOrgenhet> organiserteEnheter = emptyList();


    public OrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }


    public Optional<IOrgenhet> getById(String id) {
        return map(orgEnhetStateRepository.findById(id));
    }


    public Optional<IOrgenhet> getByFid(String fid) {
        return map(orgEnhetStateRepository.findByFid(fid));
    }


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


    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return map(orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .max(comparing(OrgEnhetState::gyldigFom))
        );
    }


    public List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds) {
        return orgEnhetStateRepository.findAllByAgressoIdIn(agressoIds).stream().map(this::map).toList();
    }


    public Collection<IOrgenhet> getAllEnheter() {
        return orgEnhetStateRepository.getAll().stream().map(this::map).toList();
    }


    public Collection<IOrgenhet> getAllEnheterMedOrganisering() {
        return organiserteEnheter;
    }


    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> orgEnhetState.agressoId().isPresent() && orgEnhetState.agressoId().get().contains(agressoId))
            .map(this::map)
            .toList();
    }


    public List<IOrgenhet> matchByNavn(String navn) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> orgEnhetState.navn().toLowerCase().contains(navn))
            .map(this::map)
            .toList();
    }


    public Map<String, IOrgenhet> getByIdBulk(List<String> ids) {
        return orgEnhetStateRepository.findByIdIn(ids).stream().collect(Collectors.toMap(OrgEnhetState::id, this::map));
    }


    public Map<String, IOrgenhet> getByFidBulk(List<String> fids) {
        return orgEnhetStateRepository.findByIdIn(fids).stream().collect(Collectors.toMap(OrgEnhetState::id, this::map));
    }


    public Collection<IOrgenhet> getByAgressoIdForEnheterMedOrganisering(String agressoId) {
        return getAllEnheterMedOrganisering().stream()
            .filter(orgenhet -> Objects.equals(orgenhet.getAgressoId(), agressoId))
            .toList();
    }


    public void setOrganiserteEnheter(List<IOrgenhet> organiserteEnheter) {
        this.organiserteEnheter = organiserteEnheter;
    }

    private Optional<IOrgenhet> map(Optional<OrgEnhetState> orgEnhetState) {
        return Optional.ofNullable(orgEnhetState.map(OrgEnhet::new).orElse(null));
    }

    private IOrgenhet map(OrgEnhetState orgEnhetState) {
        return new OrgEnhet(orgEnhetState);
    }
}

