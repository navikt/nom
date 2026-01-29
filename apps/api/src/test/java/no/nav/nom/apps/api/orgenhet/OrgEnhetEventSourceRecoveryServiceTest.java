package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventProducer;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSource;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSourceRecoveryService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSourceRepository;
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
public class OrgEnhetEventSourceRecoveryServiceTest {

    JsonMapper jsonMapper = new JsonMapper();

    public OrgEnhetEventSourceRecoveryServiceTest() {
        jsonMapper.rebuild().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
    }

    @Mock
    private JsonMapper objectMapperMock;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private OrgEnhetEventSourceRepository orgEnhetEventSourceRepository;

    @Mock
    private OrgEnhetEventProducer orgEnhetEventProducer;

    @InjectMocks
    private OrgEnhetEventSourceRecoveryService orgEnhetEventSourceRecoveryService;

    @Test
    @DisplayName("Test at eventene blir sendt - OrgEnhet")
    void test_001() {
        var orgEnhetEventSources = orgEnhetEventSources();

        when(orgEnhetEventSourceRepository.findAll()).thenReturn(orgEnhetEventSources);
        when(leaderElection.isLeader()).thenReturn(true);

        orgEnhetEventSourceRecoveryService.replayAllEventsource(false);

        verify(orgEnhetEventProducer, times(5)).replayMessage(any(), any());
    }

    private List<OrgEnhetEventSource> orgEnhetEventSources() {

        String jsonOpprettet =
            """
            {
                "fid": "orgEnhet-01K82TXX74VZZY4W4BEK4E0YWE",
                "data": {
                    "id": "py353d",
                    "navn": "FOB Barnetrygd",
                    "type": "OrgEnhetOpprettetEventV1$OrgEnhetOpprettetEventData",
                    "gyldigFom": "2012-12-01",
                    "lederRessursFid": null,
                    "overordnetOrgEnhetFid": null
                },
                "type": "OrgEnhetOpprettetEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": {
                            "lastet_dato": "2013-06-25T00:00:00Z",
                            "orgnode_niv": "ORGENHET",
                            "orgenhet_kode": "4430321",
                            "orgenhet_navn": "FOB Barnetrygd",
                            "enhettype_kode": "11",
                            "enhettype_navn": "Arbeid og Ytelser",
                            "leder_ressursnr": "109538",
                            "orgenhet_status": "P",
                            "orgenhet_fra_dato": 201212,
                            "orgenhet_til_dato": 209912,
                            "stilling_maks_gyldig_til_dato": "2013-09-01T00:00:00Z"
                        }
                    },
                    "createdTimestamp": "2025-10-21T09:32:23.398057"
                },
                "additionalData": {
                    "type": "OrgEnhetOpprettetEventV1$OrgEnhetOpprettetAdditionalAgressoData",
                    "agressoId": "4430321",
                    "agressoNavn": "FOB Barnetrygd",
                    "agressoOrgNivaa": "ORGENHET",
                    "uniqueAgressoId": "4430321-FOB Barnetrygd-ORGENHET",
                    "agressoOrgenhetType": "Arbeid og Ytelser"
                }
            }
            """;

        String jsonAvviklet =
            """
            {
                "fid": "orgEnhet-01K82TXX74VZZY4W4BEK4E0YWE",
                "data": {
                    "type": "OrgEnhetAvvikletEventV1$OrgEnhetAvvikletEventData",
                    "gyldigTom": "2013-09-01"
                },
                "type": "OrgEnhetAvvikletEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": {
                            "lastet_dato": "2013-06-25T00:00:00Z",
                            "orgnode_niv": "ORGENHET",
                            "orgenhet_kode": "4430321",
                            "orgenhet_navn": "FOB Barnetrygd",
                            "enhettype_kode": "11",
                            "enhettype_navn": "Arbeid og Ytelser",
                            "leder_ressursnr": "109538",
                            "orgenhet_status": "P",
                            "orgenhet_fra_dato": 201212,
                            "orgenhet_til_dato": 209912,
                            "stilling_maks_gyldig_til_dato": "2013-09-01T00:00:00Z"
                        }
                    },
                    "createdTimestamp": "2025-10-21T09:32:23.472332"
                },
                "additionalData": null
            }
            """;

        String jsonKorrigert =
            """
            {
                "fid": "orgEnhet-01GPJGGY5EYCH6VFPPSZ6H3DMT",
                "data": {
                    "id": "ry348j",
                    "navn": "Hms Rogaland, Ledelse/adm",
                    "type": "OrgEnhetKorrigertEventV1$OrgEnhetKorrigertEventData",
                    "gyldigFom": "2013-04-03",
                    "gyldigTom": null,
                    "lederRessursFid": null,
                    "orgEnhetsKategori": null,
                    "overordnetOrgEnhetFid": null
                },
                "type": "OrgEnhetKorrigertEventV1",
                "metadata": {
                    "inputMetadata": {
                        "origination": {
                            "initiatedBy": "nom-api",
                            "initiatedFrom": "nom-api"
                        },
                        "inputInterfaceName": "nom-api BatchJob",
                        "inputInterfaceMethodName": "Datavarehus",
                        "inputInterfaceMethodArguments": {
                            "LASTET_DATO": "2013-04-03",
                            "ORGNODE_NIV": "ORGNIV4",
                            "ORGENHET_KODE": "4711100",
                            "ORGENHET_NAVN": "Hms Rogaland, Ledelse/adm",
                            "ENHETTYPE_KODE": null,
                            "ENHETTYPE_NAVN": null,
                            "LEDER_RESSURSNR": null,
                            "ORGENHET_STATUS": "P",
                            "SIST_LASTET_DATO": "2013-04-03",
                            "ORGENHET_FRA_PERIODE": 0,
                            "ORGENHET_TIL_PERIODE": 209999,
                            "STILLING_MAKS_GYLDIG_TIL_DATO": "2009-12-31"
                        }
                    },
                    "createdTimestamp": "2023-02-17T06:00:11.328220643"
                },
                "additionalData": {
                    "type": "OrgEnhetKorrigertEventV1$OrgEnhetKorrigertAdditionalAgressoData",
                    "agressoId": "4711100",
                    "agressoNavn": "Hms Rogaland, Ledelse/adm",
                    "agressoOrgNivaa": "ORGNIV4",
                    "uniqueAgressoId": "4711100-Hms Rogaland, Ledelse/adm-ORGNIV4",
                    "agressoOrgenhetType": null
                }
            }
            """;

        String jsonTypeEndret =
            """
            {
                "fid": "orgEnhet-01GPJGHHEJTW2HSJED22SSM93K",
                "data": {
                    "type": "OrgEnhetTypeEndretEventV1$OrgEnhetTypeEndretEventData",
                    "orgEnhetsType": "DIR"
                },
                "type": "OrgEnhetTypeEndretEventV1",
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
                    "createdTimestamp": "2023-04-18T09:32:47.453483686"
                },
                "additionalData": null
            }
            """;

        String jsonNomNivaaEndret =
            """
            {
                "fid": "orgEnhet-01GPJGGW9VC3GE3XDW5GXDAGMV",
                "data": {
                    "type": "NomNivaaEndretEventV1$NomNivaaEndretEventData",
                    "nomNivaa": "DRIFTSENHET"
                },
                "type": "NomNivaaEndretEventV1",
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
                    "createdTimestamp": "2023-04-26T12:03:35.714553747"
                },
                "additionalData": null
            }
            """;

        List<OrgEnhetEventSource> jsonNodes = new ArrayList<>();
        var jsonNodeStrings = List.of(jsonOpprettet, jsonAvviklet, jsonKorrigert, jsonTypeEndret, jsonNomNivaaEndret);
        AtomicInteger count = new AtomicInteger();
        jsonNodeStrings.forEach(jsonNodeString -> {
            JsonNode jsonNode;
            try {
                jsonNode = jsonMapper.readTree(jsonNodeString);
            } catch (JacksonException e) {
                throw new RuntimeException(e);
            }
            jsonNodes.add(new OrgEnhetEventSource("event-" + count.incrementAndGet(), jsonNode));
        });
        return jsonNodes;
    }

}
