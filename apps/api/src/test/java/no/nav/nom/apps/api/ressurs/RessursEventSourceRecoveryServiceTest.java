package no.nav.nom.apps.api.ressurs;

import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventProducer;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSource;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRecoveryService;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RessursEventSourceRecoveryServiceTest {

    JsonMapper jsonMapper = new JsonMapper();

    public RessursEventSourceRecoveryServiceTest() {
        jsonMapper.rebuild().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
    }

    @Mock
    private JsonMapper objectMapperMock;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private RessursEventSourceRepository ressursEventSourceRepository;

    @Mock
    private RessursEventProducer ressursEventProducer;

    @InjectMocks
    private RessursEventSourceRecoveryService ressursEventSourceRecoveryService;

    @Test
    @DisplayName("Test at eventene blir sendt - Ressurs")
    void test_001() {
        var ressursEventSources = ressursEventSources();

        when(ressursEventSourceRepository.findAll()).thenReturn(ressursEventSources);
        when(leaderElection.isLeader()).thenReturn(true);

        ressursEventSourceRecoveryService.replayAllEventsource(false);

        verify(ressursEventProducer, times(3)).replayMessage(any(), any());
    }

    private List<RessursEventSource> ressursEventSources() {

        String jsonOpprettet =
            """
            {
                "fid": "ressurs-123",
                "data": {
                    "type": "RessursOpprettetEventV1$RessursOpprettetEventData",
                    "sektor": "EKSTERN",
                    "personident": "1234567812345",
                    "visningsnavn": {
                        "fornavn": "Nav",
                        "etternavn": "Navesen",
                        "mellomnavn": null
                    }
                },
                "type": "RessursOpprettetEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": {
                            "FNR": "1234567812345",
                            "EPOST": "Nav.Navesen@nav.no",
                            "FORNAVN": "Nav",
                            "ETTERNAVN": "Navesen",
                            "NAV_IDENT": "E123456",
                            "BUCKET_NUM": 30,
                            "ANSATT_TYPE": "Eksternt",
                            "REMEDY_AKTIV": "NEI",
                            "ANSATT_PER_IDAG": "NEI",
                            "REMEDY_PERMISJON": "NEI",
                            "REMEDY_SLUTT_DATO": "2019-01-29",
                            "REMEDY_START_DATO": "2016-06-24",
                            "AGRESSO_SLUTT_DATO": "2016-08-21",
                            "ANSATT_RESSURS_KODE": null,
                            "ANSETTELSESTYPE_BESK": null
                        }
                    },
                    "createdTimestamp": "2022-11-16T13:31:30.523280401"
                },
                "additionalData": {
                    "type": "RessursOpprettetEventV1$RessursOpprettetAdditionalData",
                    "kjoenn": null,
                    "navident": "E123456",
                    "folkeregistrertNavn": null
                }
            }
            """;

        String jsonPersonalia =
            """
            {
                "fid": "FID_MANUELL_T168579",
                "data": {
                    "type": "RessursPersonaliaOppdatertV1$PersonaliaOppdatertEventData",
                    "visningsnavn": {
                        "fornavn": "Nav",
                        "etternavn": "Navesen",
                        "mellomnavn": null
                    }
                },
                "type": "RessursPersonaliaOppdatertV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": {
                            "fnr": "1234567812345",
                            "epost": "Nav.Navesen@nav.no",
                            "fornavn": "Nav",
                            "etternavn": "Navesen",
                            "nav_ident": "S123456",
                            "ident_type": "STANDARD",
                            "ansatt_type": "NAV Statlig",
                            "remedy_aktiv": "JA",
                            "ansatt_per_idag": "JA",
                            "remedy_permisjon": "NEI",
                            "fysisk_brukersted": "2700",
                            "remedy_slutt_dato": "2099-12-31",
                            "remedy_start_dato": "2023-05-03",
                            "agresso_slutt_dato": "2099-12-31",
                            "agresso_start_dato": "2023-06-13",
                            "ansatt_ressurs_kode": "F",
                            "ansettelsestype_besk": "Fast"
                        }
                    },
                    "createdTimestamp": "2025-02-19T05:13:58.025577706"
                },
                "additionalData": null
            }
            """;

        String jsonSluttdato =
            """
            {
                "fid": "ressurs-1234",
                "data": {
                    "type": "RessursSluttdatoOppdatertEventV1$RessursSluttdatoOppdatertEventData",
                    "sluttdato": "2023-04-18"
                },
                "type": "RessursSluttdatoOppdatertEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": {
                            "fnr": "1234567812345",
                            "epost": "Nav.Navesen@nav.no",
                            "fornavn": "Nav",
                            "etternavn": "Navesen",
                            "nav_ident": "E123456",
                            "ident_type": "STANDARD",
                            "ansatt_type": "Eksternt",
                            "remedy_aktiv": "JA",
                            "ansatt_per_idag": "JA",
                            "remedy_permisjon": "JA",
                            "fysisk_brukersted": "1419",
                            "remedy_slutt_dato": "2023-04-18",
                            "remedy_start_dato": "2021-01-25",
                            "agresso_slutt_dato": "2099-12-31",
                            "agresso_start_dato": "2020-12-22",
                            "ansatt_ressurs_kode": "L",
                            "ansettelsestype_besk": null
                        }
                    },
                    "createdTimestamp": "2024-12-09T13:09:14.128290923"
                },
                "additionalData": null
            }
            """;

        List<RessursEventSource> jsonNodes = new ArrayList<>();
        var jsonNodeStrings = List.of(jsonOpprettet, jsonPersonalia, jsonSluttdato);
        AtomicInteger count = new AtomicInteger();
        jsonNodeStrings.forEach(jsonNodeString -> {
            JsonNode jsonNode;
            try {
                jsonNode = jsonMapper.readTree(jsonNodeString);
            } catch (JacksonException e) {
                throw new RuntimeException(e);
            }
            jsonNodes.add(new RessursEventSource("event-" + count.incrementAndGet(), jsonNode));
        });
        return jsonNodes;
    }
}
