package no.nav.nom.apps.api.orgenhet.kostnadssted;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.koststed.KostnadsstedMappingResponseDTO;
import no.nav.nom.apps.api.clients.koststed.OrgEnhetKostnadsstedMappingClient;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreKostnadssted;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KostnadsstedSynkroniseringServiceTest {

    private final OrgEnhetKostnadsstedMappingClient client = mock(OrgEnhetKostnadsstedMappingClient.class);
    private final OrgEnhetStateRepository repository = mock(OrgEnhetStateRepository.class);
    private final OrgEnhetCommandService commandService = mock(OrgEnhetCommandService.class);
    private final LeaderElection leaderElection = mock(LeaderElection.class);
    private KostnadsstedSynkroniseringService service;

    private static final LocalDate FOM = LocalDate.now().minusYears(1);

    @Mock
    private Unleash unleash;

    @BeforeEach
    void setUp() {
        service = new KostnadsstedSynkroniseringService(client, repository, commandService, leaderElection, unleash);
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    private static OrgEnhetState orgEnhet(String fid, String id, String overFid, String kostnadssted) {
        return OrgEnhetState.builder().fid(fid).id(id).navn(id).gyldigFom(FOM)
            .overordnetOrgEnhetFid(overFid).kostnadsstedId(kostnadssted).build();
    }

    @Test
    void should_identify_and_publish_changed_kostnadssted() {
        when(repository.getAll()).thenReturn(List.of(
            orgEnhet("FID_TOPP", "TOPP", null, "K0"),
            orgEnhet("FID_A", "A", "FID_TOPP", "K1"),
            orgEnhet("FID_B", "B", "FID_TOPP", null),
            orgEnhet("FID_C", "C", "FID_TOPP", "K3")
        ));
        when(client.hentKostnadsstedMappingerForGittToppNode("TOPP")).thenReturn(List.of(
            new KostnadsstedMappingResponseDTO("K0", "TOPP", FOM, null),
            new KostnadsstedMappingResponseDTO("K1_NY", "A", FOM, null),
            new KostnadsstedMappingResponseDTO("K2", "B", FOM, null),
            new KostnadsstedMappingResponseDTO("K_UTLOPT", "C", FOM, LocalDate.now().minusDays(10))
        ));

        var endringer = service.synkroniserKostnadssteder();

        assertThat(endringer).containsExactlyInAnyOrder(
            new KostnadsstedSynkroniseringService.KostnadsstedEndring("FID_A", "A", "K1", "K1_NY"),
            new KostnadsstedSynkroniseringService.KostnadsstedEndring("FID_B", "B", null, "K2")
        );
        verify(commandService).endreKostnadssted(eq("FID_A"), argThat((EndreKostnadssted c) ->
            c.id().equals("A") && c.kostnadsstedId().equals("K1_NY")));
        verify(commandService).endreKostnadssted(eq("FID_B"), argThat((EndreKostnadssted c) ->
            c.id().equals("B") && c.kostnadsstedId().equals("K2")));
        verify(commandService, never()).endreKostnadssted(eq("FID_TOPP"), any());
        verify(commandService, never()).endreKostnadssted(eq("FID_C"), any());
    }

    @Test
    void should_not_run_when_not_leader() {
        when(leaderElection.isLeader()).thenReturn(false);

        service.synkroniserKostnadsstederScheduler();

        verifyNoInteractions(client, repository, commandService);
    }
}

