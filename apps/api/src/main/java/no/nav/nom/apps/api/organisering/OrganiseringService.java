package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.AvsluttOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrganiseringService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrganiseringStateRepository organiseringStateRepository;

    private final OrgenhetService orgenhetService;

    private final ObjectMapper objectMapper;

    private final OrganiseringEventCommandService organiseringEventCommandService;

    public OrganiseringService(
        OrganiseringStateRepository organiseringStateRepository, OrgenhetService orgenhetService,
        OrganiseringEventCommandService organiseringEventCommandService,
        ObjectMapper objectMapper
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgenhetService = orgenhetService;
        this.organiseringEventCommandService = organiseringEventCommandService;
        this.objectMapper = objectMapper;
    }

    public void saveOrganiseringer(List<DvhOrganisering> dvhOrganiseringer) {
        logger.info("Starter oppdatering av organiseringer evtSrc");

        if(dvhOrganiseringer.isEmpty()) {
            logger.warn("Ingen organiseringer mottatt fra DVH, så ingen organiseringer vil bli endret.");
            return;
        }

        int numberOfNew = 0;
        int numberOfTerminated = 0;
        AtomicInteger numberOfTerminatedNotFromDvh = new AtomicInteger();

        List<OrganiseringIdTuple> organiseringIdsFromDvh = new ArrayList<>();
        List<String> organiseringerTerminatedByUpdate = new ArrayList<>();


        for (DvhOrganisering dvhOrganisering : dvhOrganiseringer) {
            logger.debug("Organisering fra DVH: {}", dvhOrganisering);

            Optional<IOrgenhet> orgEnhet = orgenhetService
                .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder());

            Optional<IOrgenhet> orgEnhetOver = orgenhetService
                .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdOver(), dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver());

            // Ved første gangs innlesing mangler nomId-er over/under i OrgEnhetState, så hopp over
            if (orgEnhet.isPresent() && orgEnhetOver.isPresent()) {
                Optional<OrganiseringState> organiseringState = getOrganiseringState(dvhOrganisering);

                if (organiseringState.isPresent()) {
                    if (hasOrganiseringChangedOrExpired(organiseringState.get().fid(), orgEnhet.get(), orgEnhetOver.get())) {
                        organiseringEventCommandService.avsluttOrganisering(
                            organiseringState.get().fid(),
                            getAvsluttOrganiseringCmd(organiseringState.get().gyldigFom(), organiseringState.get())
                        );
                        numberOfTerminated++;
                        organiseringerTerminatedByUpdate.add(organiseringState.get().fid());

                        organiseringEventCommandService.opprettOrganisering(getOpprettOrganiseringCmd(orgEnhet.get(), orgEnhetOver.get(), dvhOrganisering));
                        numberOfNew++;
                    }
                } else {
                    organiseringEventCommandService.opprettOrganisering(getOpprettOrganiseringCmd(orgEnhet.get(), orgEnhetOver.get(), dvhOrganisering));
                    numberOfNew++;
                }
                organiseringIdsFromDvh.add(new OrganiseringIdTuple(orgEnhet.get().getId(), orgEnhetOver.get().getId()));
            } else {
                logger.info("orgEnhet eller orgEnhetOver ikke finnes i orgenhetStateRepository. orgEnhet: ({} {} {}) orgEnhetOver: ({} {} {})",
                    dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder(),
                    dvhOrganisering.getAgressoIdOver(), dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver());
            }
        }

        logger.info("Antall organiseringer fra DVH: {} Antall nye organiseringer: {} Antall avsluttede: {}", dvhOrganiseringer.size(), numberOfNew, numberOfTerminated);

        List<OrganiseringState> organiseringerToTerminate = organiseringerInStateNotInDvhOrganiseringer(
            organiseringIdsFromDvh,
            organiseringStateRepository.getActiveOrganisering()
        );
        // Remove organiseringer we have already generated avslutt events for
        organiseringerToTerminate.stream()
            .filter(organiseringState -> !organiseringerTerminatedByUpdate.contains(organiseringState.fid()))
            .peek(organiseringState -> numberOfTerminatedNotFromDvh.getAndIncrement())
            .forEach(organiseringState -> organiseringEventCommandService.avsluttOrganisering(
                organiseringState.fid(),
                getAvsluttOrganiseringCmd(organiseringState.gyldigFom(), organiseringState)
            ));

        logger.info("Antall organiseringer ikke lenger mottatt fra DVH og derfor avsluttet: {} ", numberOfTerminatedNotFromDvh.get());

        organiseringStateRepository.logRepositoryState();
    }

    public record OrganiseringIdTuple(
        String id,
        String idOver
    ) {}

    // Brukes kun  av DvhLast - og det er fine - kun eksisterende aktive fid for orgenhet som skal behandles
    private Optional<OrganiseringState> getOrganiseringState(DvhOrganisering dvhOrganisering) {
        Optional<IOrgenhet> orgEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
            dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivUnder(),
            dvhOrganisering.getNavnUnder()
        );
        if (orgEnhet.isEmpty()) {
            return Optional.empty();
        }
        return organiseringStateRepository.findActiveById(orgEnhet.get().getId());
    }

    public List<OrganiseringState> organiseringerInStateNotInDvhOrganiseringer(
        List<OrganiseringIdTuple> organiseringerFromDvh,
        Map<String, OrganiseringState> organiseringerInState
    ) {
        return organiseringerInState.values().stream()
            .filter(organiseringState -> !organiseringerFromDvh.contains(new OrganiseringIdTuple(organiseringState.id(), organiseringState.idOver())))
            .toList();
    }

    public boolean hasOrganiseringChangedOrExpired(String fid, IOrgenhet newOrgEnhet, IOrgenhet newOrgEnhetOver) {
        boolean organiseringEndret = false;

        if (fid.startsWith("organisering")) {
            Optional<OrganiseringState> eksisterendeAktivOrganisering = organiseringStateRepository.findActiveByFid(fid);
            // hvis organiseringen er aktiv
            if (eksisterendeAktivOrganisering.isPresent()) {
                OrganiseringState eksisterendeOrg = eksisterendeAktivOrganisering.get();

                if (!eksisterendeOrg.id().equals(newOrgEnhet.getId()) ||
                    !eksisterendeOrg.idOver().equals(newOrgEnhetOver.getId())) {
                    organiseringEndret = true;
                }
            } else {
                Optional<OrganiseringState> eksisterendeOrganisering = organiseringStateRepository.findByFid(fid);
                // hvis organiseringen er inaktiv
                if(eksisterendeOrganisering.isPresent()){
                    organiseringEndret = true;
                }
            }
        }
        return organiseringEndret;
    }

    @SuppressWarnings("unused")
    private EndreOrganiseringCmd getEndreOrganiseringCmd(IOrgenhet orgenhet, IOrgenhet orgenhetOver, DvhOrganisering dvhOrganisering) {
        return new EndreOrganiseringCmd(
            orgenhet.getId(),
            orgenhet.getNavn(),
            orgenhetOver.getId(),
            orgenhetOver.getNavn(),
            dvhOrganisering.getGyldigFom(), // TODO Fix dates!
            null,
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private OpprettOrganiseringCmd getOpprettOrganiseringCmd(IOrgenhet orgenhet, IOrgenhet orgenhetOver, DvhOrganisering dvhOrganisering) {
        return new OpprettOrganiseringCmd(
            orgenhet.getId(),
            orgenhet.getNavn(),
            orgenhetOver.getId(),
            orgenhetOver.getNavn(),
            LocalDate.now(),
            null,
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private AvsluttOrganiseringCmd getAvsluttOrganiseringCmd(LocalDate gyldigFom, OrganiseringState organiseringState) {
        return new AvsluttOrganiseringCmd(
            (gyldigFom == null || gyldigFom.isEqual(LocalDate.now())) ? LocalDate.now() : LocalDate.now().minusDays(1),
            createInputMetadata(getRawData(organiseringState))
        );
    }

    public JsonNode getRawData(OrganiseringState organiseringState) {
        return this.objectMapper.valueToTree(organiseringState);
    }

    private InputMetadata createInputMetadata() {
        return createInputMetadata(null);
    }

    private InputMetadata createInputMetadata(Object rawData) {
        return new InputMetadata(
            "nom-api BatchJob",
            "Datavarehus",
            rawData,
            new Origination("nom-api", "nom-api")
        );
    }

}
