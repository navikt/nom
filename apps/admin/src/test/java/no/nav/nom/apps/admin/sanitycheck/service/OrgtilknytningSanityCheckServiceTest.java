package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgtilknytningSanityCheckServiceTest {

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;

    private OrgtilknytningSanityCheckService service;

    private final LocalDate now = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new OrgtilknytningSanityCheckService(orgtilknytningStateRepository, orgEnhetStateRepository);
    }


    @Test
    void orgtilknytninger_som_ikke_overlapper_skal_ikke_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusYears(1).plusDays(1))
                .gyldigTom(Optional.of(now.plusYears(2)))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer).isEmpty();
    }

    @Test
    void orgtilknytninger_med_overlapp_skal_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusMonths(2))
                .gyldigTom(Optional.of(now.plusYears(2)))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer.size()).isEqualTo(1);
    }

    @Test
    void orgtilknytninger_som_ikke_er_daglig_oppfolging_skal_ikke_vaere_med_i_sammenligning() {
        // OT2 er identisk med OT1, men ikke DOL
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .erDagligOppfolging(false)
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer).isEmpty();
    }

    @Test
    void orgtilknytninger_i_fortiden_skal_ikke_vaere_med_i_sammenligning() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.minusYears(1))
                .gyldigTom(Optional.of(now.minusYears(1).plusMonths(6)))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer).isEmpty();
    }

    @Test
    void alle_overlappende_orgtilknytninger_skal_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusMonths(2))
                .gyldigTom(Optional.of(now.plusYears(2)))
                .build();
        var orgTilknytning3 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-456def")
                .gyldigFom(now.plusMonths(4))
                .gyldigTom(Optional.of(now.plusYears(3)))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2, orgTilknytning3));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer.size()).isEqualTo(2);
    }

    @Test
    void duplikate_orgtilknytninger_skal_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var duplikateOTer = service.duplikateOrgtilknytninger();

        assertThat(duplikateOTer.size()).isEqualTo(1);
    }

    @Test
    void orgtilknytninger_som_ikke_er_like_skal_ikke_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusMonths(6))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var duplikateOTer = service.duplikateOrgtilknytninger();

        assertThat(duplikateOTer).isEmpty();
    }
    

    private OrgtilknytningState orgTilknytning() {
        return OrgtilknytningState.builder()
                .fid("orgtilknytning-acb123")
                .navident("A123456")
                .orgEnhetId("ab123c")
                .erDagligOppfolging(true)
                .gyldigFom(now)
                .gyldigTom(Optional.of(now.plusYears(1)))
                .build();
    }
}