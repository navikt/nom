package no.nav.nom.apps.api.orgenhet;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedAzpName;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Service
public class OrgenhetService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final DvhRepository dvhRepository;
    private final KafkaTopicReader kafkaTopicReader;
    private final OrganiseringStateRepository organiseringStateRepository;
    private List<IOrgenhet> organiserteEnheter = emptyList();


    public OrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository, 
                           OrgEnhetCommandService orgEnhetCommandService, 
                           DvhRepository dvhRepository, 
                           KafkaTopicReader kafkaTopicReader, 
                           OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.dvhRepository = dvhRepository;
        this.kafkaTopicReader = kafkaTopicReader;
        this.organiseringStateRepository = organiseringStateRepository;
    }


    public Optional<IOrgenhet> getById(String id) {
        return orgEnhetStateRepository.findById(id).map(this::map);
    }


    public Optional<IOrgenhet> getByFid(String fid) {
        return orgEnhetStateRepository.findByFid(fid).map(this::map);
    }


    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                    .stream()
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(comparing(OrgEnhetState::gyldigFom))
                .map(this::map);
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }


    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .max(comparing(OrgEnhetState::gyldigFom))
            .map(this::map);
    }


    public List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds) {
        return orgEnhetStateRepository.findAllByAgressoIdIn(agressoIds).stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .map(this::map).toList();
    }


    public Collection<IOrgenhet> getAllEnheter() {
        return orgEnhetStateRepository.getAll().stream().map(this::map).toList();
    }

    // Returns orgEnheter that 
    // - has a parent unit (is considered active)
    // - has no children (is a leaf node in the tree)
    public Collection<IOrgenhet> getAllOrgEnheterThatCanHaveRessurser() {
        return orgEnhetStateRepository.getAll().stream()
                .filter(orgEnhetState -> organiseringStateRepository.orgEnhetHasParent(orgEnhetState.id()))
                .filter(orgEnhetState -> !organiseringStateRepository.orgEnhetHasChildren(orgEnhetState.id()))
                .map(this::map).toList();
    }

    public Collection<IOrgenhet> getAllOrgEnheterThatCanHaveRessurser(String orgEnhetId) {
        var underliggendeOrgEnhet = getAllUnderliggendeOrgEnhetById(orgEnhetId).stream().map(iOrgenhet -> orgEnhetStateRepository.findById(iOrgenhet.getId()).get()).toList();
        return underliggendeOrgEnhet.stream()
            .filter(orgEnhetState -> organiseringStateRepository.orgEnhetHasParent(orgEnhetState.id()))
            .filter(orgEnhetState -> !organiseringStateRepository.orgEnhetHasChildren(orgEnhetState.id()))
            .map(this::map).toList();
    }

    public Collection<IOrgenhet> getAllEnheterMedOrganisering() {
        return organiserteEnheter;
    }


    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> !orgEnhetState.endedOnStartDate() && orgEnhetState.agressoId().isPresent() && orgEnhetState.agressoId().get().contains(agressoId))
            .map(this::map)
            .toList();
    }

    public List<IOrgenhet> exactMatchByAgressoId(String agressoId) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState ->
                !orgEnhetState.endedOnStartDate()
                    && orgEnhetState.agressoId().isPresent()
                    && orgEnhetState.agressoId().get().equalsIgnoreCase(agressoId))
            .map(this::map)
            .toList();
    }

    public Map<String, IOrgenhet> getByIdBulk(List<String> ids) {
        return orgEnhetStateRepository.findByIdIn(ids).stream().collect(Collectors.toMap(OrgEnhetState::id, this::map));
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

    private IOrgenhet map(OrgEnhetState orgEnhetState) {
        return new OrgEnhet(orgEnhetState);
    }

    public List<String> findAllUnderliggendeOrgenheter(String id) {
        logger.debug("Finner underliggende org.enheter for {} med ny implementasjon", id);

        Set<String> underliggendeIds = orgEnhetStateRepository.findById(id).map(parent -> {

            List<OrganiseringState> organiseringer = organiseringStateRepository.getOrganiseringerContainingId(parent.id());
            ArrayList<String> allUnderliggende = new java.util.ArrayList<>(emptyList());

            Set<OrganiseringState> nextLevelChildren = organiseringer.stream().filter(organisering -> organisering.idOver().equals(parent.id()))
                .collect(Collectors.toSet());

            while (!nextLevelChildren.isEmpty()) {
                allUnderliggende.addAll(nextLevelChildren.stream().map(OrganiseringState::id).toList());
                nextLevelChildren = findChildOrganiseringerUsingNewOrganisering(nextLevelChildren);
            }

            return new HashSet<>(allUnderliggende);
        }).orElse(new HashSet<>());

        return orgEnhetStateRepository.findByIdIn(underliggendeIds).stream().map(OrgEnhetState::id).toList();
    }

    public List<IOrgenhet> getAllUnderliggendeOrgEnhetById(String id) {
        Set<String> underliggendeIds = orgEnhetStateRepository.findById(id).map(parent -> {

            List<OrganiseringState> organiseringer = organiseringStateRepository.getOrganiseringerContainingId(parent.id());
            ArrayList<String> allUnderliggende = new java.util.ArrayList<>(emptyList());

            Set<OrganiseringState> nextLevelChildren = organiseringer.stream().filter(organisering -> organisering.idOver().equals(parent.id()))
                .collect(Collectors.toSet());

            while (!nextLevelChildren.isEmpty()) {
                allUnderliggende.addAll(nextLevelChildren.stream().map(OrganiseringState::id).toList());
                nextLevelChildren = findChildOrganiseringerUsingNewOrganisering(nextLevelChildren);
            }

            return new HashSet<>(allUnderliggende);
        }).orElse(new HashSet<>());

        return orgEnhetStateRepository.findByIdIn(underliggendeIds).stream().map(this::map).toList();
    }

    public boolean isOrgEnhetInArbeidsomraade(String orgenhetId) {
        var orgenhetState = orgEnhetStateRepository.findById(orgenhetId);
        if (orgenhetState.isEmpty()) {
            return false;
        }

        if (orgenhetState.get().nomNivaa().isPresent() && orgenhetState.get().nomNivaa().get().equals(NomNivaa.ARBEIDSOMRAADE)) {
            return true;
        }

        var organisering = organiseringStateRepository.findActiveById(orgenhetState.get().id());

        return organisering.filter(organiseringState -> isOrgEnhetInArbeidsomraade(organiseringState.idOver())).isPresent();
    }

    public boolean isOrgenhetInDriftsenhet(String orgenhetId) {
        var orgenhetState = orgEnhetStateRepository.findById(orgenhetId);
        if (orgenhetState.isEmpty()) {
            return false;
        }

        if (orgenhetState.get().nomNivaa().isPresent() && orgenhetState.get().nomNivaa().get().equals(NomNivaa.DRIFTSENHET)) {
            return true;
        }

        var organisering = organiseringStateRepository.findActiveById(orgenhetState.get().id());

        return organisering.filter(organiseringState -> isOrgenhetInDriftsenhet(organiseringState.idOver())).isPresent();
    }

    private Set<OrganiseringState> findChildOrganiseringerUsingNewOrganisering(Set<OrganiseringState> parentOrganiseringer) {
        return parentOrganiseringer.stream()
            .map(organisering ->
                organiseringStateRepository.getOrganiseringerContainingId(organisering.id()).stream()
                    .filter(lowerOrganisering ->
                        !lowerOrganisering.equals(organisering) &&
                            lowerOrganisering.idOver().equals(organisering.id())
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
                        new Origination(getAuthenticatedNavIdent(), getAuthenticatedAzpName()))
                )
            );
        }
    }

    public List<JsonNode> getOrgEnhetEvents(ReadTopicRequest readTopicRequest) {
        return kafkaTopicReader.getOrgEnhetEvents(readTopicRequest.maxRecords(), readTopicRequest.fromOffset(), readTopicRequest.partition());
    }
}
