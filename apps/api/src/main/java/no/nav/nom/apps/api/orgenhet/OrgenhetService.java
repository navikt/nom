package no.nav.nom.apps.api.orgenhet;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
import static no.nav.nom.apps.api.organisering.OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE;

@Service
public class OrgenhetService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final DvhRepository dvhRepository;
    private final KafkaTopicReader kafkaTopicReader;
    private final Unleash unleash;
    private final OrganiseringStateRepository organiseringStateRepository;
    private List<IOrgenhet> organiserteEnheter = emptyList();


    public OrgenhetService(OrgEnhetStateRepository orgEnhetStateRepository, OrgEnhetCommandService orgEnhetCommandService, DvhRepository dvhRepository, KafkaTopicReader kafkaTopicReader,
                           Unleash unleash,
                           OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.dvhRepository = dvhRepository;
        this.kafkaTopicReader = kafkaTopicReader;
        this.unleash = unleash;
        this.organiseringStateRepository = organiseringStateRepository;
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
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(comparing(OrgEnhetState::gyldigFom))
            );
        }
        return getEnhetByAgressoIdAndOrgniv(agressoId, orgNiv);
    }


    public Optional<IOrgenhet> getEnhetByAgressoIdAndOrgniv(String agressoId, String orgNiv) {
        return map(orgEnhetStateRepository.findAllByAgressoIdAndNiv(agressoId + "--" + orgNiv)
            .stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .max(comparing(OrgEnhetState::gyldigFom))
        );
    }


    public List<IOrgenhet> findEnheterByAgressoIdIn(Set<String> agressoIds) {
        return orgEnhetStateRepository.findAllByAgressoIdIn(agressoIds).stream()
            .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
            .map(this::map).toList();
    }


    public Collection<IOrgenhet> getAllEnheter() {
        return orgEnhetStateRepository.getAll().stream().map(this::map).toList();
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
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
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

        } else {
            logger.debug("Finner underliggende org.enheter for {} med gammel implementasjon", id);

            Set<String> underliggendeAgressoIds = orgEnhetStateRepository.findById(id).map(parent -> {
                if (parent.agressoId().isEmpty() | parent.agressoOrgNivaa().isEmpty()) {
                    return Collections.<String>emptySet();
                }
                var organiseringer = dvhRepository.getOrganiseringer(parent.agressoId().get(), parent.agressoOrgNivaa().get(), parent.navn());
                var allUnderliggende = new java.util.ArrayList<String>(emptyList());

                var nextLevelChildren = organiseringer.stream()
                    .filter(organisering -> organisering.getAgressoIdOver().equals(parent.agressoId().get()))
                    .collect(
                        Collectors.toSet());


                while (!nextLevelChildren.isEmpty()) {
                    allUnderliggende.addAll(nextLevelChildren.stream().map(DvhOrganisering::getAgressoIdUnder).toList());
                    nextLevelChildren = findChildOrganiseringer(nextLevelChildren);
                }

                return new HashSet<>(allUnderliggende);
            }).orElse(emptySet());

            return orgEnhetStateRepository.findAllByAgressoIdIn(underliggendeAgressoIds).stream().map(OrgEnhetState::id).toList();
        }
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

    private Set<OrganiseringState> findChildOrganiseringerUsingNewOrganisering(Set<OrganiseringState> parentOrganiseringer) {
        return parentOrganiseringer.stream()
            .map(organisering ->
                organiseringStateRepository.getOrganiseringerContainingId(organisering.id()).stream()
                    .filter(lowerOrganisering ->
                        !lowerOrganisering.equals(organisering) &&
                            lowerOrganisering.id().equals(organisering.id())
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

    public List<JsonNode> getOrgEnhetEvents(ReadTopicRequest readTopicRequest) {
        return kafkaTopicReader.getOrgEnhetEvents(readTopicRequest.maxRecords(), readTopicRequest.fromOffset(), readTopicRequest.partition());
    }
}
