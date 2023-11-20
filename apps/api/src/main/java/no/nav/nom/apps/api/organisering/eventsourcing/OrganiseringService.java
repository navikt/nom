package no.nav.nom.apps.api.organisering.eventsourcing;

import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.AvsluttOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrganiseringState;
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

    private final OrganiseringEventCommandService organiseringEventCommandService;

    public OrganiseringService(
        OrganiseringStateRepository organiseringStateRepository, OrgenhetService orgenhetService,
        OrganiseringEventCommandService organiseringEventCommandService
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgenhetService = orgenhetService;
        this.organiseringEventCommandService = organiseringEventCommandService;
    }

    public void saveOrganiseringer(List<DvhOrganisering> dvhOrganiseringer) {
        logger.info("Starter innlesning av organiseringer");

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
                Optional<String> organiseringFid = getFidForOrganisering(dvhOrganisering);

                if (organiseringFid.isPresent()) {
                    if (hasOrganiseringChanged(organiseringFid.get(), dvhOrganisering, orgEnhet.get(), orgEnhetOver.get())) {
                        organiseringEventCommandService.avsluttOrganisering(organiseringFid.get(), getAvsluttOrganiseringCmd());
                        numberOfTerminated++;
                        organiseringerTerminatedByUpdate.add(organiseringFid.get());

                        organiseringEventCommandService.opprettOrganisering(
                            getOpprettOrganiseringCmd(orgEnhet.get(), orgEnhetOver.get(), dvhOrganisering));
                        numberOfNew++;
                    }
                } else {
                    organiseringEventCommandService.opprettOrganisering(getOpprettOrganiseringCmd(orgEnhet.get(), orgEnhetOver.get(), dvhOrganisering));
                    numberOfNew++;
                }

                organiseringIdsFromDvh.add(new OrganiseringIdTuple(orgEnhet.get().getId(), orgEnhetOver.get().getId()));

            } else {
                logger.info("orgEnhet eller orgEnhetOver ikke finnes i orgenhetStateRepository. orgEnhet: {} orgEnhetOver: {}", orgEnhet, orgEnhetOver);
            }
        }

        logger.info("Antall nye organiseringer: {} Antall avsluttede: {}", numberOfNew, numberOfTerminated);


        List<String> fidsToTerminate = fidsInStateNotInDvhOrganiseringer(organiseringIdsFromDvh, organiseringStateRepository.getAll());
        // Remove organiseringer we have already generated avslutt events for
        fidsToTerminate.stream()
            .filter(fid -> !organiseringerTerminatedByUpdate.contains(fid))
            .peek(fid -> numberOfTerminatedNotFromDvh.getAndIncrement())
            .forEach(fid -> organiseringEventCommandService.avsluttOrganisering(fid, getAvsluttOrganiseringCmd()));

        logger.info("Antall organiseringer ikke lenget mottatt fra DVH og derfor avsluttet: {} ", numberOfTerminatedNotFromDvh.get());
    }

    public record OrganiseringIdTuple(String id, String idOver) {}

    private Optional<String> getFidForOrganisering(DvhOrganisering dvhOrganisering) {
        return Optional.ofNullable(organiseringStateRepository
            .findFidByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()));
    }

    public List<String> fidsInStateNotInDvhOrganiseringer(List<OrganiseringIdTuple> organiseringerFromDvh, Map<String, OrganiseringState> organiseringerInState) {
        return organiseringerInState.entrySet().stream()
            .filter(e -> !organiseringerFromDvh.contains(new OrganiseringIdTuple(e.getValue().id(), e.getValue().idOver())))
            .map(Map.Entry::getKey)
            .toList();
    }

    public boolean hasOrganiseringChanged(String fid, DvhOrganisering newOrganiseringFromDvh, IOrgenhet newOrgEnhet, IOrgenhet newOrgEnhetOver) {
        boolean organiseringEndret = false;

        if (fid.startsWith("organisering")) {
            Optional<OrganiseringState> eksisterendeOrganisering = organiseringStateRepository.findByFid(fid);

            if (eksisterendeOrganisering.isPresent()) {
                OrganiseringState eksisterendeOrg = eksisterendeOrganisering.get();

                if (!eksisterendeOrg.id().equals(newOrgEnhet.getId()) ||
                    !eksisterendeOrg.idOver().equals(newOrgEnhetOver.getId()) ||
                    !eksisterendeOrg.navn().equals(newOrganiseringFromDvh.getNavnUnder()) ||
                    !eksisterendeOrg.navnOver().equals(newOrganiseringFromDvh.getNavnOver()) ||
                    !eksisterendeOrg.gyldigFom().equals(newOrganiseringFromDvh.getGyldigFom()) ||
                    !eksisterendeOrg.gyldigTom().equals(Optional.ofNullable(newOrganiseringFromDvh.getGyldigTom()))
                ) {
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
            dvhOrganisering.getGyldigTom(),
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private OpprettOrganiseringCmd getOpprettOrganiseringCmd(IOrgenhet orgenhet, IOrgenhet orgenhetOver, DvhOrganisering dvhOrganisering) {
        return new OpprettOrganiseringCmd(
            orgenhet.getId(),
            orgenhet.getNavn(),
            orgenhetOver.getId(),
            orgenhetOver.getNavn(),
            dvhOrganisering.getGyldigFom(), // TODO Fix dates!
            dvhOrganisering.getGyldigTom(),
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private AvsluttOrganiseringCmd getAvsluttOrganiseringCmd() {
        return new AvsluttOrganiseringCmd(
            LocalDate.now(), // TODO Fix dates!
            createInputMetadata()
        );
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
