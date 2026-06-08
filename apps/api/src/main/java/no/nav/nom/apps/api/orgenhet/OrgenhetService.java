package no.nav.nom.apps.api.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreTilgangsenhet;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
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
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedAzpName;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.libs.utils.common.util.DateUtils.isPeriodActive;

@Slf4j
@Service
public class OrgenhetService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final KafkaTopicReader kafkaTopicReader;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final AppProperties.Orgenhet orgenhetProperties;
    private final AktiveOrgenheterIHierarkiCache aktiveOrgenheterIHierarkiCache;

    public OrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository,
                           OrgEnhetCommandService orgEnhetCommandService,
                           KafkaTopicReader kafkaTopicReader,
                           OrganiseringStateRepository organiseringStateRepository,
                           AktiveOrgenheterIHierarkiCache aktiveOrgenheterIHierarkiCache,
                           AppProperties appProperties
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.kafkaTopicReader = kafkaTopicReader;
        this.organiseringStateRepository = organiseringStateRepository;
        this.aktiveOrgenheterIHierarkiCache = aktiveOrgenheterIHierarkiCache;
        this.orgenhetProperties = appProperties.orgenhet();
    }


    public Optional<IOrgenhet> getById(String id) {
        return orgEnhetStateRepository.findById(id).map(OrgEnhet::new);
    }

    public Optional<IOrgenhet> getByFid(String fid) {
        return orgEnhetStateRepository.findByFid(fid).map(OrgEnhet::new);
    }

    public Optional<IOrgenhet> getEnhetByAgressoIdNivaAndName(String agressoId, String orgNiv, String name) {
        if (name != null) {
            return orgEnhetStateRepository.findAllByUniqueAgressoId(agressoId + "-" + name + "-" + orgNiv)
                    .stream()
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(comparing(OrgEnhetState::gyldigFom))
                .map(OrgEnhet::new);
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }

    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .max(comparing(OrgEnhetState::gyldigFom))
            .map(OrgEnhet::new);
    }

    public Collection<IOrgenhet> getAllEnheter() {
        return orgEnhetStateRepository.getAll().stream().map(OrgEnhet::new).collect(Collectors.toList());
    }

    public Collection<IOrgenhet> getAktiveEnheterIOrganisasjonen() {
        return aktiveOrgenheterIHierarkiCache.getAktiveEnheterIOrganisasjonen();
    }

    // Returns orgEnheter that 
    // - has a parent unit (is considered active)
    // - has no children (is a leaf node in the tree)
    public Collection<IOrgenhet> getAllOrgEnheterThatCanHaveRessurser() {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> organiseringStateRepository.orgEnhetHasParent(orgEnhetState.id()))
            .filter(orgEnhetState -> !organiseringStateRepository.orgEnhetHasChildren(orgEnhetState.id()))
            .map(OrgEnhet::new)
            .filter(iOrgenhet -> !iOrgenhet.getId().equals(orgenhetProperties.vikafossenId()))
            .collect(Collectors.toList());
    }

    public Collection<IOrgenhet> getAllOrgEnheterThatCanHaveRessurser(String orgEnhetId) {
        var underliggendeOrgEnhet = getAllUnderliggendeOrgEnhetById(orgEnhetId).stream()
            .map(iOrgenhet -> orgEnhetStateRepository.findById(iOrgenhet.getId()).get())
            .toList();
        return underliggendeOrgEnhet.stream()
            .filter(orgEnhetState -> organiseringStateRepository.orgEnhetHasParent(orgEnhetState.id()))
            .filter(orgEnhetState -> !organiseringStateRepository.orgEnhetHasChildren(orgEnhetState.id()))
            .map(OrgEnhet::new)
            .collect(Collectors.toList());
    }

    public boolean isOrgEnhetBeforeLeafNode(String orgEnhetId, Collection<IOrgenhet> orgEnheter) {
        return orgEnheter.stream()
            .map(IOrgenhet::getId)
            .map(organiseringStateRepository::findActiveById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .allMatch(organiseringState -> organiseringState.idOver().equals(orgEnhetId));
    }



    public boolean activeOrFutureWithEqualName(String name) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> orgEnhetState.isActive() || orgEnhetState.gyldigFom().isAfter(LocalDate.now()))
            .anyMatch(orgEnhetState -> orgEnhetState.navn().equalsIgnoreCase(name));
    }


    public List<IOrgenhet> matchByAgressoId(String agressoId) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> !orgEnhetState.endedOnStartDate() && nonNull(orgEnhetState.agressoId()) && orgEnhetState.agressoId().contains(agressoId))
            .map(OrgEnhet::new)
            .collect(Collectors.toList());
    }

    public List<IOrgenhet> exactMatchByAgressoId(String agressoId) {
        return orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState ->
                !orgEnhetState.endedOnStartDate()
                    && nonNull(orgEnhetState.agressoId())
                    && orgEnhetState.agressoId().equalsIgnoreCase(agressoId))
            .map(OrgEnhet::new)
            .collect(Collectors.toList());
    }

    public Map<String, IOrgenhet> getByIdBulk(List<String> ids) {
        return orgEnhetStateRepository.findByIdIn(ids).stream().collect(Collectors.toMap(OrgEnhetState::id, OrgEnhet::new));
    }

    public List<IOrgenhet> getByAgressoIdForEnheterMedOrganisering(String agressoId) {
        return getAktiveEnheterIOrganisasjonen().stream()
            .filter(orgenhet -> Objects.equals(orgenhet.getAgressoId(), agressoId))
            .toList();
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

        return orgEnhetStateRepository.findByIdIn(underliggendeIds).stream().map(OrgEnhet::new).collect(Collectors.toList());
    }

    public boolean isOrgEnhetInArbeidsomraade(String orgenhetId) {
        var orgenhetState = orgEnhetStateRepository.findById(orgenhetId);
        if (orgenhetState.isEmpty()) {
            return false;
        }

        if (Objects.equals(orgenhetState.get().nomNivaa(), NomNivaa.ARBEIDSOMRAADE)) {
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

        if (Objects.equals(orgenhetState.get().nomNivaa(), NomNivaa.DRIFTSENHET)) {
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
        boolean existingOrgEnhetsTypeIsDifferentFromNewValueOrNotPresent =
            isNull(orgEnhetState.orgEnhetsType()) || orgEnhetState.orgEnhetsType() != orgEnhetsType;

        if (existingOrgEnhetsTypeIsDifferentFromNewValueOrNotPresent) {
            orgEnhetCommandService.endreOrgEnhetsType(
                orgEnhetState.fid(),
                new EndreOrgEnhetsType(
                    orgEnhetsType,
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

    public String getTilgangsenhetIdOver(String nomId) {
        var organiseringOrgEnhetId = organiseringStateRepository.findActiveById(nomId).map(OrganiseringState::idOver).orElse("");
        var orgEnhetOver = getById(organiseringOrgEnhetId);
        if (orgEnhetOver.isPresent() && (isNull(orgEnhetOver.get().getTilgangsenhetId()) || orgEnhetOver.get().getTilgangsenhetId().isBlank())) {
            return getTilgangsenhetIdOver(orgEnhetOver.get().getId());
        }
        return orgEnhetOver.map(IOrgenhet::getTilgangsenhetId).orElse(null);
    }

    public String getTilgangsenhetIdUnder(String nomId) {
        return getAllUnderliggendeOrgEnhetById(nomId).stream()
            .filter(orgEnhet -> nonNull(orgEnhet.getTilgangsenhetId()))
            .findFirst().map(IOrgenhet::getTilgangsenhetId).orElse(null);
    }


    @KafkaListener(
        id = "nomApi_organiseringState_listener_for_tilgangsenhet_resolve",
        containerFactory = "nomStateOrganiseringKafkaListenerContainerFactory",
        topics = "${app.kafka.organisering.stateTopic}"
    )
    public void onMessage(ConsumerRecords<String, OrganiseringState> records) {
        InputMetadata inputMetadata = new InputMetadata(
            "nom-api BatchJob",
            "oppdaterTilgansenhet - OrgenhetService",
            null,
            new Origination("nom-api", "nom-api")
        );
        StreamSupport.stream(records.spliterator(), false).forEach(record -> {
            var organiseringState = record.value();
            if (!isPeriodActive(organiseringState.gyldigFom(), organiseringState.gyldigTom())) {
                return;
            }
            var orgEnhetState = orgEnhetStateRepository.findById(organiseringState.id())
                .orElseThrow(() -> OrgEnhetNotFoundException.id(organiseringState.id()));

            if (isNull(orgEnhetState.tilgangsenhetId()) || orgEnhetState.tilgangsenhetId().isBlank()) {
                var orgEnhetOver = orgEnhetStateRepository.findById(organiseringState.idOver())
                    .orElseThrow(() -> new OrgEnhetNotFoundException(organiseringState.idOver()));

                String tilgansenhetId = orgEnhetOver.tilgangsenhetId();

                if (isNull(tilgansenhetId) || tilgansenhetId.isBlank()) {
                    tilgansenhetId = getTilgangsenhetIdOver(orgEnhetOver.id());
                }

                if (isNull(tilgansenhetId) || tilgansenhetId.isBlank()) {
                    tilgansenhetId = getTilgangsenhetIdUnder(orgEnhetState.id());
                }

                orgEnhetCommandService.endreTilgangsenhet(
                    orgEnhetState.fid(),
                    new EndreTilgangsenhet(tilgansenhetId, inputMetadata.toBuilder().inputInterfaceMethodArguments(orgEnhetOver).build())
                );
            }
        });
    }
}
