package no.nav.nom.apps.api.orgenhetleder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederEventProducer;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup.OrgEnhetLederEventSource;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup.OrgEnhetLederEventSourceRecoveryService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup.OrgEnhetLederEventSourceRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrgEnhetLederEventSourceRecoveryServiceTest {

    ObjectMapper objectMapper = new ObjectMapper();

    public OrgEnhetLederEventSourceRecoveryServiceTest() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Mock
    private ObjectMapper objectMapperMock;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private OrgEnhetLederEventSourceRepository orgEnhetLederEventSourceRepository;

    @Mock
    private OrgEnhetLederEventProducer orgEnhetLederEventProducer;

    @InjectMocks
    private OrgEnhetLederEventSourceRecoveryService orgEnhetLederEventSourceRecoveryService;

    @Test
    @DisplayName("Test at eventene blir sendt - OrgEnhetLeder")
    void test_001() {
        var orgEnhetLederEventSource = orgEnhetLederEventSources();

        when(orgEnhetLederEventSourceRepository.findAll()).thenReturn(orgEnhetLederEventSource);
        when(leaderElection.isLeader()).thenReturn(true);

        orgEnhetLederEventSourceRecoveryService.replayAllEventsource(false);

        verify(orgEnhetLederEventProducer, times(3)).replayMessage(any(), any());
    }

    private List<OrgEnhetLederEventSource> orgEnhetLederEventSources() {
        String jsonOpprettet =
            """
            {
                "fid": "leder-01K6CY18F58CQ5HXX1GZ5JKF4A",
                "data": {
                    "type": "OrgEnhetLederOpprettetEventV1$OrgEnhetLederOpprettetEventData",
                    "navident": "A100221",
                    "orgEnhetId": "na701s"
                },
                "type": "OrgEnhetLederOpprettetEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "persisterLedereForOrgenheter",
                        "inputInterfaceMethodArguments": null
                    },
                    "createdTimestamp": "2025-09-30T11:07:39.623211936"
                },
                "additionalData": null
            }
            """;

        String jsonStart =
            """
            {
                "fid": "leder-01K6CY18F58CQ5HXX1GZ5JKF4A",
                "data": {
                    "type": "OrgEnhetLederStartdatoOppdatertEventV1$OrgEnhetLederStartdatoOppdatertData",
                    "startdato": "2025-09-30"
                },
                "type": "OrgEnhetLederStartdatoOppdatertEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "persisterLedereForOrgenheter",
                        "inputInterfaceMethodArguments": null
                    },
                    "createdTimestamp": "2025-09-30T11:07:39.927703011"
                },
                "additionalData": null
            }
            """;

        String jsonSlutt =
            """
            {
                "fid": "leder-01K6CY7Z78ASBBVVR4WPE819PJ",
                "data": {
                    "type": "OrgEnhetLederSluttdatoOppdatertEventV1$OrgEnhetLederAvsluttetEventData",
                    "sluttdato": "2025-09-29"
                },
                "type": "OrgEnhetLederSluttdatoOppdatertEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "persisterLedereForOrgenheter",
                        "inputInterfaceMethodArguments": null
                    },
                    "createdTimestamp": "2025-09-30T16:14:34.741577089"
                },
                "additionalData": null
            }
            """;

        List<OrgEnhetLederEventSource> jsonNodes = new ArrayList<>();
        var jsonNodeStrings = List.of(jsonOpprettet, jsonStart, jsonSlutt);
        AtomicInteger count = new AtomicInteger();
        jsonNodeStrings.forEach(jsonNodeString -> {
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(jsonNodeString);
            } catch (JsonProcessingException jpe) {
                throw new RuntimeException(jpe);
            }
            jsonNodes.add(new OrgEnhetLederEventSource("event-" + count.getAndIncrement(), jsonNode));
        });
        return jsonNodes;
    }
}
