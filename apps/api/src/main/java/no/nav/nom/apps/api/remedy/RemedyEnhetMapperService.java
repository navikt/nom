package no.nav.nom.apps.api.remedy;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgEnhetOverordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RemedyEnhetMapperService {

    public static final Logger logger = LogManager.getLogger(RemedyEnhetMapperService.class);

    final private Resource resourceFile;
    final private OrgenhetService orgenhetService;
    final private OrgEnhetOverordnetResolver overordnetResolver;
    private List<RemedyEnhet> remedyEnhetList;


    public RemedyEnhetMapperService(
        @Value("classpath:remedy/data.csv") Resource resourceFile,
        @Autowired OrgenhetService orgenhetService,
        @Autowired OrgEnhetOverordnetResolver overordnetResolver
    ) {
        this.resourceFile = resourceFile;
        this.orgenhetService = orgenhetService;
        this.overordnetResolver = overordnetResolver;

        init();
    }

    private void init() {
        logger.info("Initiating RemedyEnhetRepository");

        RemedyEnhetCsvReader reader = new RemedyEnhetCsvReader(resourceFile);
        remedyEnhetList = reader.readRows();

        logger.info("RemedyEnhetRepository is populated");
    }


    public Optional<RemedyEnhet> findByNomId(String nomOrgEnhetId) {

        var remedyEnhetWithNomId = remedyEnhetList.stream().filter(remedyEnhet -> nomOrgEnhetId.equals(remedyEnhet.nomId())).findFirst();

        if (remedyEnhetWithNomId.isPresent()) {
            return remedyEnhetWithNomId;
        } else {
            var remedyEnhet = findRemedyEnhet(nomOrgEnhetId);

            if (remedyEnhet.isPresent()) {
                return Optional.of(
                    new RemedyEnhet(
                        remedyEnhet.get().remedyEnhetId(),
                        remedyEnhet.get().agressoId(),
                        remedyEnhet.get().orgNivaa(),
                        nomOrgEnhetId
                    ));
            } else {
                logger.debug("Kunne ikke finne remedyEnhet for nomId {} ", nomOrgEnhetId);
            }
        }

        return Optional.empty();
    }

    private Optional<RemedyEnhet> findRemedyEnhet(String nomOrgEnhetId) {
        var currentOptionalOrgEnhet = orgenhetService.getById(nomOrgEnhetId);

        while (currentOptionalOrgEnhet.isPresent()) {
            final var orgEnhet = currentOptionalOrgEnhet.get();

            var remedyEnhet = remedyEnhetList.stream()
                .filter(r -> r.agressoId().equals(orgEnhet.getAgressoId()) && r.orgNivaa().equals(orgEnhet.getOrgNiv()))
                .findFirst();

            if (remedyEnhet.isPresent()) {
                return remedyEnhet;
            } else {
                //TODO: replace to this when OverordnetOrgEnhetFid property is in state
                //currentOptionalOrgEnhet = orgEnhet.getOverordnetOrgEnhetFid().flatMap(o -> orgenhetService.getByFid(o));
                currentOptionalOrgEnhet = overordnetResolver.findOverordnetOrgEnhet(orgEnhet.getFid()).map(OrgEnhet::new);
            }
        }

        return Optional.empty();
    }

    public Optional<RemedyEnhet> findByRemedyEnhetId(String remedyEnhetId) {
        var remedyEnhet = remedyEnhetList.stream().filter(enhet -> remedyEnhetId.equals(enhet.remedyEnhetId())).findFirst();

        if (remedyEnhet.isPresent()) {
            if (!StringUtils.isBlank(remedyEnhet.get().nomId())) {
                return remedyEnhet;
            } else {
                var nomId = orgenhetService
                    .getEnhetByAgressoIdAndOrgniv(remedyEnhet.get().agressoId(), remedyEnhet.get().orgNivaa())
                    .map(IOrgenhet::getId);

                if (nomId.isPresent()) {
                    return Optional.of(
                        new RemedyEnhet(
                            remedyEnhet.get().remedyEnhetId(),
                            remedyEnhet.get().agressoId(),
                            remedyEnhet.get().orgNivaa(),
                            nomId.get()
                        ));
                } else {
                    logger.debug("Kunne ikke fine orgEnhet nomId for remedyEnhetId {} med agressoId {} og orgenhetnivå {}",
                        remedyEnhet.get().remedyEnhetId(), remedyEnhet.get().agressoId(), remedyEnhet.get().orgNivaa()
                    );
                }
            }
        }

        return Optional.empty();
    }

}
