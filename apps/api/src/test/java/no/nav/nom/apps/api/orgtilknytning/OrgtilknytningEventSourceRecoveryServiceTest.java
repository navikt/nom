package no.nav.nom.apps.api.orgtilknytning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSource;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSourceRecoveryService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSourceRepository;
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
public class OrgtilknytningEventSourceRecoveryServiceTest {

    ObjectMapper objectMapper = new ObjectMapper();

    public OrgtilknytningEventSourceRecoveryServiceTest() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Mock
    private ObjectMapper objectMapperMock;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private OrgtilknytningEventSourceRepository repository;

    @Mock
    private OrgtilknytningEventProducer producer;

    @InjectMocks
    private OrgtilknytningEventSourceRecoveryService service;

    @Test
    @DisplayName("Test at eventene blir sendt - OrgEnhet")
    void test_001() {
        var orgtilknytningEventSources = orgtilknytningEventSources();

        when(repository.findAll()).thenReturn(orgtilknytningEventSources);
        when(leaderElection.isLeader()).thenReturn(true);

        service.replayAllEventsources(false);

        verify(producer, times(3)).replayMessage(any(), any());
    }

    private List<OrgtilknytningEventSource> orgtilknytningEventSources() {
        String jsonOpprettet =
            """
            {
                "fid": "orgtilknytning-01H2WJAPWTDFKF3H84GYKEYPFE",
                "data": {
                    "type": "OrgtilknytningOpprettetEventV1$OrgtilknytningOpprettetEventData",
                    "navident": "S153372",
                    "gyldigFom": "2021-07-05",
                    "orgEnhetId": "hy645d"
                },
                "type": "OrgtilknytningOpprettetEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": null
                    },
                    "createdTimestamp": "2023-06-14T11:03:33.274565894"
                },
                "additionalData": {
                    "type": "OrgtilknytningEvent$AdditionalAgressoData",
                    "hrresStillingRessursEnhet": "HRRES_STILLING[1502004-Molde Tett oppfølging]/HRRES_RESSURS[1502004-ORGENHET]",
                    "hrresStillingerPerioderFormatert": "HRRES_STILLING[0335002-Ullern Oppfølging 2]/HRRES_RESSURS[1502004-ORGENHET]: 2019-09-01 - 2021-07-04 for nom orgenhet [he999z-Ullern Oppfølging 2]\\nHRRES_STILLING[1502004-Molde Tett oppfølging]/HRRES_RESSURS[1502004-ORGENHET]: 2021-07-05 - 2099-12-31 for nom orgenhet [hy645d-Molde Tett oppfølging]\\n"
                }
            }
            """;

        String jsonDOL =
            """
            {
                "fid": "orgtilknytning-01H2WJAYH6W38PEZJ75A33243P",
                "data": {
                    "type": "OrgtilknytningDagligOppfolgingEndretEventV1$OrgtilknytningDagligOppfolgingEndretEventData",
                    "navident": "L124123",
                    "gyldigFom": "2020-01-15",
                    "gyldigTom": null,
                    "orgEnhetId": "hu954y",
                    "dagligOppfolging": true
                },
                "type": "OrgtilknytningDagligOppfolgingEndretEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": null
                    },
                    "createdTimestamp": "2023-06-14T11:03:41.099449026"
                },
                "additionalData": {
                    "type": "OrgtilknytningEvent$AdditionalAgressoData",
                    "hrresStillingRessursEnhet": "HRRES_STILLING[0220001-Asker Arbeid og levekår ung 1]/HRRES_RESSURS[0220001-ORGENHET]",
                    "hrresStillingerPerioderFormatert": "HRRES_STILLING[0220001-Asker Arbeid og levekår ung 1]/HRRES_RESSURS[0220001-ORGENHET]: 2020-01-15 - 2099-12-31 for nom orgenhet [hu954y-Asker Arbeid og levekår ung 1]\\n"
                }
            }
            """;

        String jsonAvsluttet =
            """
            {
                "fid": "orgtilknytning-01H2WJF90PNV27X3W60TRCJ1R5",
                "data": {
                    "type": "OrgtilknytningAvsluttetEventV1$OrgtilknytningAvsluttetEventData",
                    "navident": "B141487",
                    "gyldigTom": "2022-02-18",
                    "orgEnhetId": "gu811a"
                },
                "type": "OrgtilknytningAvsluttetEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": null
                    },
                    "createdTimestamp": "2023-06-27T14:20:08.102642184"
                },
                "additionalData": {
                    "type": "OrgtilknytningEvent$AdditionalAgressoData",
                    "hrresStillingRessursEnhet": "HRRES_STILLING[0124006-Indre Østfold Ungdom]/HRRES_RESSURS[0124006-ORGENHET]",
                    "hrresStillingerPerioderFormatert": "HRRES_STILLING[0124006-Indre Østfold Ungdom]/HRRES_RESSURS[0124006-ORGENHET]: 2020-04-01 - 2099-12-31 for nom orgenhet [gu811a-Indre Østfold Ungdom]\\n"
                }
            }
            """;

        List<OrgtilknytningEventSource> jsonNodes = new ArrayList<>();
        var jsonNodeStrings = List.of(jsonOpprettet, jsonDOL, jsonAvsluttet);
        AtomicInteger count = new AtomicInteger();
        jsonNodeStrings.forEach(jsonNodeString -> {
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(jsonNodeString);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            jsonNodes.add(new OrgtilknytningEventSource("event-" + count.incrementAndGet(), jsonNode));
        });
        return jsonNodes;
    }
}
