package no.nav.nom.apps.api.organisering.eventsourcing;

import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
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

import java.util.List;
import java.util.Optional;

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
        int numberOfUpdated = 0;

        for (DvhOrganisering dvhOrganisering : dvhOrganiseringer) {
            Optional<IOrgenhet> orgEnhetUnder = orgenhetService
                .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder());

            Optional<IOrgenhet> orgEnhetOver = orgenhetService
                .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdOver(), dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver());

            // Ved første gangs innlesing mangler nomId-er over/under i OrgEnhetState, så hopp over
            if (orgEnhetUnder.isPresent() && orgEnhetOver.isPresent()) {
                Optional<String> organiseringFid = getFidForOrganisering(dvhOrganisering);

                if (organiseringFid.isPresent()) {
                    if (erOrganiseringEndret(organiseringFid.get(), dvhOrganisering, orgEnhetUnder.get(), orgEnhetOver.get())) {
                        //if organisering is changed
                        organiseringEventCommandService.endreOrganisering(organiseringFid.get(), getEndreOrganiseringCmd(dvhOrganisering));
                        numberOfUpdated++;
                    }
                } else {
                    var opprettetFid = organiseringEventCommandService.opprettOrganisering(getOpprettOrganiseringCmd(dvhOrganisering));
                    addToStateRepository(opprettetFid, dvhOrganisering);
                    numberOfNew++;
                }
            }

        }

        logger.info("Antall nye organiseringer: {} Antall oppdaterte: {}", numberOfNew, numberOfUpdated);
    }


    private void addToStateRepository(String fid, DvhOrganisering dvhOrganisering) {
        var nomIdUnder = orgenhetService
            .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder());

        var nomIdOver = orgenhetService
            .getEnhetByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdOver(), dvhOrganisering.getOrgNivOver(), dvhOrganisering.getNavnOver());

        if (nomIdUnder.isEmpty() || nomIdOver.isEmpty()) {
            logger.info("Kan ikke lagre organisering med fid={} agressoIdOver={} agressoIdUnder={} i organisering state, mangler nom ids (over={} under={})",
                fid, dvhOrganisering.getAgressoIdOver(), dvhOrganisering.getAgressoIdUnder(), nomIdOver, nomIdUnder
            );
            return;
        }

        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid(fid)
            .idUnder(nomIdUnder.get().getId())
            .idOver(nomIdOver.get().getId())
            .navnUnder(dvhOrganisering.getNavnUnder())
            .navnOver(dvhOrganisering.getNavnOver())
            .gyldigFom(dvhOrganisering.getGyldigFom())
            .gyldigTom(Optional.ofNullable(dvhOrganisering.getGyldigTom()))
            .build();

        organiseringStateRepository.put(fid, organiseringState);
    }

    private Optional<String> getFidForOrganisering(DvhOrganisering dvhOrganisering) {
        return Optional.ofNullable(organiseringStateRepository
            .findFidByAgressoIdNivaAndName(dvhOrganisering.getAgressoIdUnder(), dvhOrganisering.getOrgNivUnder(), dvhOrganisering.getNavnUnder()));
    }

    public boolean erOrganiseringEndret(String fid, DvhOrganisering dvhOrg, IOrgenhet orgEnhetUnder, IOrgenhet orgEnhetOver) {
        // TODO
        boolean organiseringEndret = false;

        if (fid.startsWith("organisering")) {
            Optional<OrganiseringState> eksisterendeOrganisering = organiseringStateRepository.findByFid(fid);

            if (eksisterendeOrganisering.isPresent()) {
                OrganiseringState eksisterendeOrg = eksisterendeOrganisering.get();

                if (!eksisterendeOrg.idUnder().equals(orgEnhetUnder.getId()) ||
                    !eksisterendeOrg.idOver().equals(orgEnhetOver.getId()) ||
                    !eksisterendeOrg.navnUnder().equals(dvhOrg.getNavnUnder()) ||
                    !eksisterendeOrg.navnOver().equals(dvhOrg.getNavnOver()) ||
                    !eksisterendeOrg.gyldigFom().equals(dvhOrg.getGyldigFom()) ||
                    !eksisterendeOrg.gyldigTom().equals(Optional.ofNullable(dvhOrg.getGyldigTom()))
                ) {
                    organiseringEndret = true;
                }
            }
        }
        return organiseringEndret;
    }

    private EndreOrganiseringCmd getEndreOrganiseringCmd(DvhOrganisering dvhOrganisering) {
        return new EndreOrganiseringCmd(
            dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivOver(),
            dvhOrganisering.getOrgNivUnder(),
            dvhOrganisering.getNavnOver(),
            dvhOrganisering.getNavnUnder(),
            dvhOrganisering.getGyldigFom(),
            dvhOrganisering.getGyldigTom(),
            createInputMetadata(dvhOrganisering.getRawData())
        );
    }

    private OpprettOrganiseringCmd getOpprettOrganiseringCmd(DvhOrganisering dvhOrganisering) {
        return new OpprettOrganiseringCmd(
            dvhOrganisering.getAgressoIdOver(),
            dvhOrganisering.getAgressoIdUnder(),
            dvhOrganisering.getOrgNivOver(),
            dvhOrganisering.getOrgNivUnder(),
            dvhOrganisering.getNavnOver(),
            dvhOrganisering.getNavnUnder(),
            dvhOrganisering.getGyldigFom(),
            dvhOrganisering.getGyldigTom(),
            createInputMetadata(dvhOrganisering.getRawData())
        );
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
