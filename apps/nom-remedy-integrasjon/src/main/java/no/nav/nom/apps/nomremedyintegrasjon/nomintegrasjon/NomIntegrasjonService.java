package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.fifteenCharLengthZeroPaddedNumber;

@Service
public class NomIntegrasjonService {

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonService.class);

    private final NomIntegrasjonRepository nomIntegrasjonRepository;

    private final NomApiClient nomApiClient;

    private final ObjectMapper objectMapper;

    public NomIntegrasjonService(NomIntegrasjonRepository nomIntegrasjonRepository, NomApiClient nomApiClient, ObjectMapper objectMapper) {
        this.nomIntegrasjonRepository = nomIntegrasjonRepository;
        this.nomApiClient = nomApiClient;
        this.objectMapper = objectMapper;
    }

    public void handleRessursEvent(RessursEvent ressursEvent) {
        if (ressursEvent instanceof RessursOpprettetEventV1 ressursOpprettetEventV1
            && ressursOpprettetEventV1.getRessursOpprettetEventData().sektor().equals(Sektor.EKSTERN)) {
            createRessurs(ressursOpprettetEventV1);
        }
    }

    private void createRessurs(RessursOpprettetEventV1 ressursOpprettetEventV1) {
        var ressursOpprettetEventData = ressursOpprettetEventV1.getRessursOpprettetEventData();
        var ressursOpprettetAdditionalData = ressursOpprettetEventV1.getRessursOpprettetAdditionalData();
        Map<String, Object> inputInterfaceMethodArguments = objectMapper.convertValue(
            ressursOpprettetEventV1.getMetadata().inputMetadata().inputInterfaceMethodArguments(),
            new TypeReference<>() {}
        );

        logger.info("Creating ressurs for fid: " + ressursOpprettetEventV1.getFid());

        var remedyEnhetsId = getRemedyEnhetId(
            Optional.ofNullable((String) inputInterfaceMethodArguments.get("orgEnhetId")).orElseThrow(() -> new NoSuchElementException("Missing value for 'orgEnhetId'"))
        );

        NomIntegrasjonEntity newNomIntegrasjonEntity = NomIntegrasjonEntity.builder()
            .entryId(nextId())
            .entryBestiller(ressursOpprettetEventV1.getMetadata().inputMetadata().origination().initiatedBy())
            .entryOperasjon("Ny")
            .entryStatus("Ubehandlet")
            .enhetsId(remedyEnhetsId)
            .etternavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().etternavn()))
            .fodselsnummer(ressursOpprettetEventData.personident())
            .fornavn(capitalizeNavn(ressursOpprettetEventData.visningsnavn().getFornavnMellomnavn()))
            .fysiskBrukerstedId("TODO") // TODO
            .mobil("TODO") // TODO
            .navId(ressursOpprettetAdditionalData.navident().orElseThrow(() -> new NoSuchElementException("Missing value for 'navident'")))
            .sektor(nomSektorToRemedyCode(ressursOpprettetEventData.sektor()))
            .tiltredelsesdato(ressursOpprettetAdditionalData.startdato().map(Utils::epochSeconds).orElseThrow(() -> new NoSuchElementException("Missing value for 'startDato'")))
            .utlopsdato(0L)
            .build();

        nomIntegrasjonRepository.save(newNomIntegrasjonEntity);

        logger.info("Created ressurs for fid: " + ressursOpprettetEventV1);
    }

    private String getRemedyEnhetId(String orgEnhetId) {
        return nomApiClient.getOrgEnhetWithRemedyId(orgEnhetId)
            .flatMap(orgEnhetWithRemedyId -> Optional.ofNullable(orgEnhetWithRemedyId.remedyEnhetId()))
            .orElseThrow(() -> new NoSuchElementException(String.format("Could not find remedyEnhetId for orgEnhet with id %s", orgEnhetId)));
    }

    // https://nav-it.slack.com/archives/C040RV7KCH5/p1696498426739059
    protected String nextId() {
        return nomIntegrasjonRepository.findTopByOrderByEntryIdDesc()
            .map(latestEntry -> Long.parseLong(latestEntry.entryId))
            .map(latestId -> fifteenCharLengthZeroPaddedNumber(latestId + 1))
            .orElse(fifteenCharLengthZeroPaddedNumber(0L));
    }

    protected Integer nomSektorToRemedyCode(Sektor sektor) {
        return switch (sektor) {
            case NAV_STATLIG -> 0;
            case NAV_KOMMUNAL -> 1;
            case EKSTERN -> 2;
        };
    }
}
