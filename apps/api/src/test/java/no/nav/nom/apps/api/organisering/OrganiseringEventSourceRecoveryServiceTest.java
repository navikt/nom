package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventProducer;
import no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup.OrganiseringEventSource;
import no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup.OrganiseringEventSourceRecoveryService;
import no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup.OrganiseringEventSourceRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrganiseringEventSourceRecoveryServiceTest {

    ObjectMapper objectMapper = new ObjectMapper();

    public OrganiseringEventSourceRecoveryServiceTest() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Mock
    private ObjectMapper objectMapperMock;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private OrganiseringEventSourceRepository organiseringEventSourceRepository;

    @Mock
    private OrganiseringEventProducer organiseringEventProducerTemplate;

    @InjectMocks
    private OrganiseringEventSourceRecoveryService organiseringEventSourceRecoveryService;

    @Test
    @DisplayName("Test at eventene blir sendt - Organisering")
    void test_001() {
        var organiseringEventSourceJsonNodes = organiseringEventsourceJsonNodes();
        var organiseringOpprettetEventSource = new OrganiseringEventSource("event-1", organiseringEventSourceJsonNodes.getFirst());
        var organiseringAvsluttetEventSource = new OrganiseringEventSource("event-2", organiseringEventSourceJsonNodes.getLast());
        var organiseringEventSources = List.of(organiseringOpprettetEventSource, organiseringAvsluttetEventSource);

        when(organiseringEventSourceRepository.findAll()).thenReturn(organiseringEventSources);
        when(leaderElection.isLeader()).thenReturn(true);

        organiseringEventSourceRecoveryService.replayAllEventsource(false);

        verify(organiseringEventProducerTemplate, times(2)).replayMessage(any(), any());
    }

    private List<JsonNode> organiseringEventsourceJsonNodes() {
        String json1 = """
                       {
                           "fid": "organisering-01HNZBGEZHMMY2FMJWE6SZ1VFT",
                           "data": {
                               "id": "ku316c",
                               "navn": "Øygarden",
                               "type": "OrganiseringOpprettetEventV1$OrganiseringOpprettetEventData",
                               "idOver": "ge554j",
                               "navnOver": "Øygarden",
                               "gyldigFom": "2024-02-06",
                               "gyldigTom": null
                           },
                           "type": "OrganiseringOpprettetEventV1",
                           "metadata": {
                               "inputMetadata": {
                                   "origination": {
                                       "initiatedBy": "nom-api",
                                       "initiatedFrom": "nom-api"
                                   },
                                   "inputInterfaceName": "nom-api BatchJob",
                                   "inputInterfaceMethodName": "Datavarehus",
                                   "inputInterfaceMethodArguments": {
                                       "ORGNODE_KODE": "1246000",
                                       "ORGNODE_NAVN": "Øygarden",
                                       "ORGNODE_NIVA": "ORGENHET",
                                       "LEDER_NAVIDENT": "A100384",
                                       "ORGNODE_STATUS": "N",
                                       "ORGNODE_KODE_OVER": "124600",
                                       "ORGNODE_NAVN_OVER": "Øygarden",
                                       "ORGNODE_NIVA_OVER": "ORGNIV3",
                                       "agresso_orgnode_type": "31"
                                   }
                               },
                               "createdTimestamp": "2024-02-06T15:00:19.69946504"
                           },
                           "additionalData": null
                       }
                       """;

        String json2 = """
                       {
                           "fid": "organisering-01HNZBGNX1SBDFCBDCYDDFK99F",
                           "data": {
                               "type": "OrganiseringAvsluttetEventV1$OrganiseringAvsluttetEventData",
                               "gyldigTom": "2024-12-11"
                           },
                           "type": "OrganiseringAvsluttetEventV1",
                           "metadata": {
                               "inputMetadata": {
                                   "origination": {
                                       "initiatedBy": "nom-api",
                                       "initiatedFrom": "nom-api"
                                   },
                                   "inputInterfaceName": "nom-api BatchJob",
                                   "inputInterfaceMethodName": "Datavarehus",
                                   "inputInterfaceMethodArguments": {
                                       "id": "ge458k",
                                       "fid": "organisering-01HNZBGNX1SBDFCBDCYDDFK99F",
                                       "navn": "Grünerløkka Oppfølging helse",
                                       "idOver": "my619v",
                                       "navnOver": "Grünerløkka strategisk leder 4",
                                       "gyldigFom": "2024-02-06",
                                       "gyldigTom": null
                                   }
                               },
                               "createdTimestamp": "2024-12-12T13:15:57.603163968"
                           },
                           "additionalData": null
                       }
                       """;

        List<JsonNode> jsonNodes = new ArrayList<>();
        var jsonNodeStrings = List.of(json1, json2);
        jsonNodeStrings.forEach(jsonNodeString -> {
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(jsonNodeString);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            jsonNodes.add(jsonNode);
        });
        return jsonNodes;
    }
}
