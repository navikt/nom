package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;

@Service
public class OrgenhetService {
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final DvhRepository dvhRepository;
    private List<IOrgenhet> organiserteEnheter = emptyList();


    public OrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository, OrgEnhetCommandService orgEnhetCommandService, DvhRepository dvhRepository) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.dvhRepository = dvhRepository;
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

    public List<IOrgenhet> getEnheterByLeder(String lederNavIdent) {
        return dvhRepository.getLedere().stream()
            .filter(l -> l.getNavIdent().equals(lederNavIdent))
            .map(l -> getEnhetByAgressoIdNivaAndName(l.getOrgEnhetNr(), l.getOrgNiv(), l.getOrgenhetNavn()).orElse(null))
            .filter(Objects::nonNull)
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

    public List<String> findAllUnderliggendeOrgenheter(String id) {
        Set<String> underliggendeAgressoIds = orgEnhetStateRepository.findById(id).map(parent -> {
            if (parent.agressoId().isEmpty() | parent.agressoOrgNivaa().isEmpty() ) {
                return Collections.<String>emptySet();
            }
            var organiseringer = dvhRepository.getOrganiseringer(parent.agressoId().get(), parent.agressoOrgNivaa().get(), parent.navn());
            var allUnderliggende = new java.util.ArrayList<String>(emptyList());

            var nextLevelChildren = organiseringer.stream().filter(organisering -> organisering.getAgressoIdOver().equals(parent.agressoId().get())).collect(
                Collectors.toSet());


            while (!nextLevelChildren.isEmpty()) {
                allUnderliggende.addAll(nextLevelChildren.stream().map(DvhOrganisering::getAgressoIdUnder).toList());
                nextLevelChildren = findChildOrganiseringer(nextLevelChildren);
            }

            return new HashSet<>(allUnderliggende);
        }).orElse(emptySet());

        return orgEnhetStateRepository.findAllByAgressoIdIn(underliggendeAgressoIds).stream().map(OrgEnhetState::id).toList();
    }

    private Set<DvhOrganisering> findChildOrganiseringer(Set<DvhOrganisering> parentOrganiseringer) {
        return parentOrganiseringer.stream()
            .map(organisering ->
                dvhRepository.getOrganiseringer(organisering.getAgressoIdUnder(), organisering.getOrgNivUnder(), organisering.getNavnUnder())
                    .stream()
                    .filter(
                        lowerOrganisering ->
                            !lowerOrganisering.equals(organisering) &&
                                lowerOrganisering.getAgressoIdOver().equals(organisering.getAgressoIdUnder())
                    ).toList()
            ).flatMap(List::stream).collect(Collectors.toSet());
    }

    public ResponseEntity<StandardResponse> setOrgEnhetsType(SetOrgEnhetsTypeRequest setOrgEnhetsTypeRequest) {
        return orgEnhetStateRepository.findById(setOrgEnhetsTypeRequest.getNomId())
            .map(orgEnhet -> {

                sendEndreOrgEnhetsTypeEvent(orgEnhet, setOrgEnhetsTypeRequest.getOrgEnhetsType());

                return ResponseEntity.ok().body(
                    StandardResponse.builder().message(String.format(
                            "Oppdatert orgEnhetsType for %s", setOrgEnhetsTypeRequest.getNomId()
                        ))
                        .build()
                );
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                StandardResponse.builder().message("NotFound").build()
            ));
    }

    private void sendEndreOrgEnhetsTypeEvent(OrgEnhetState orgEnhetState, OrgEnhetsType orgEnhetsType) {
        var existingOrgEnhetsTypeIsDifferentFromNewValueOrNotPresent = orgEnhetState.orgEnhetsType()
            .map(existingOrgenhetsType -> existingOrgenhetsType != orgEnhetsType)
            .orElse(true);

        if (existingOrgEnhetsTypeIsDifferentFromNewValueOrNotPresent) {
            orgEnhetCommandService.endreOrgEnhetsType(
                orgEnhetState.fid(),
                new EndreOrgEnhetsType(
                    Optional.ofNullable(orgEnhetsType),
                    new InputMetadata(
                        "nom-api REST",
                        "setOrgEnhetsType",
                        null,
                        new Origination("nom-api", "nom-api"))
                )
            );
        }
    }
}

