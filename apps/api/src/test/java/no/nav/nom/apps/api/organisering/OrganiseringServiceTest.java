package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganiseringServiceTest {

    @Mock
    private OrganiseringStateRepository organiseringStateRepositoryMock;

    @InjectMocks
    private OrganiseringService organiseringService;


    @Test
    @DisplayName("Returnere false når det ikke er noen endring")
    void should_return_false_when_nothing_has_changed() {
        String fid = "organisering-xyz123";
        String nomIdUnder = "nomIdUnder";
        String nomIdOver = "nomIdOver";

        // Initial organisering from DVH
        DvhOrganisering initialDvhOrg = initialDvhOrganisering();

        // Setup response from organisering state lookup
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomIdUnder, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet dvhOrgenhetUnder = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdUnder)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // No change to OrgEnhet over
        OrgEnhet dvhOrgenhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdOver)
            .navn(initialDvhOrg.getNavnOver())
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, initialDvhOrg, dvhOrgenhetUnder, dvhOrgenhetOver)).isFalse();
    }

    @Test
    @DisplayName("Returnere true når navnOver er endret")
    void should_return_true_when_navnOver_has_changed() {
        String fid = "organisering-xyz123";
        String nomIdUnder = "nomIdUnder";
        String nomIdOver = "nomIdOver";

        String newNavnOver = "newNavnOver";
        String newNomIdOver = "newNomIdOver";

        // Initial organisering from DVH
        DvhOrganisering initialDvhOrg = initialDvhOrganisering();

        // New organisering from DVH
        DvhOrganisering newDvhOrg = initialDvhOrganisering().toBuilder()
            .navnOver(newNavnOver)
            .build();

        // Setup response from organisering state lookup
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomIdUnder, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet dvhOrgenhetUnder = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdUnder)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // OrgEnhet over has changed
        OrgEnhet dvhOrgenhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(newNomIdOver)
            .navn(newNavnOver)
            .build());

        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, dvhOrgenhetUnder, dvhOrgenhetOver)).isTrue();
    }

    @Test
    @DisplayName("Returnere true når orgNivOver er endret")
    void should_return_true_when_orgNivOver_has_changed() {
        String fid = "organisering-xyz123";
        String nomIdUnder = "nomIdUnder";
        String nomIdOver = "nomIdOver";

        String newOrgNivOver = "newOrgNivOver";
        String newNomIdOver = "newNomIdOver";

        // Initial organisering from DVH
        DvhOrganisering initialDvhOrg = initialDvhOrganisering();

        // New organisering from DVH
        DvhOrganisering newDvhOrg = initialDvhOrganisering().toBuilder()
            .orgNivOver(newOrgNivOver)
            .build();

        // Setup response from organisering state lookup
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomIdUnder, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet dvhOrgenhetUnder = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdUnder)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // OrgEnhet over has changed
        OrgEnhet dvhOrgenhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(newNomIdOver)
            .navn(initialDvhOrg.getNavnOver())
            .agressoOrgNivaa(Optional.of(newOrgNivOver))
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, dvhOrgenhetUnder, dvhOrgenhetOver)).isTrue();
    }

    @Test
    @DisplayName("Returnere true når agressoId er endret")
    void should_return_true_when_agressoId_has_changed() {
        String fid = "organisering-xyz123";
        String nomIdUnder = "nomIdUnder";
        String nomIdOver = "nomIdOver";

        String newNomIdOver = "newNomIdOver";
        String newAgressoIdOver = "newAgressoIdOver";

        // Initial organisering from DVH
        DvhOrganisering initialDvhOrg = initialDvhOrganisering();

        // New organisering from DVH
        DvhOrganisering newDvhOrg = initialDvhOrganisering().toBuilder()
            .agressoIdOver(newAgressoIdOver)
            .build();

        // Setup response from organisering state lookup
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomIdUnder, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet dvhOrgenhetUnder = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdUnder)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // OrgEnhet over has changed
        OrgEnhet dvhOrgenhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(newNomIdOver)
            .navn(initialDvhOrg.getNavnOver())
            .agressoId(Optional.of(newAgressoIdOver))
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, dvhOrgenhetUnder, dvhOrgenhetOver)).isTrue();
    }

    private void mockOrganiseringStateLookup(String fid, DvhOrganisering initialDvhOrg, String nomIdUnder, String nomIdOver) {
        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid(fid)
            .idUnder(nomIdUnder)
            .idOver(nomIdOver)
            .navnUnder(initialDvhOrg.getNavnUnder())
            .navnOver(initialDvhOrg.getNavnOver())
            .gyldigTom(Optional.empty())
            .gyldigFom(LocalDate.now().minusDays(1))
            .build();

        when(organiseringStateRepositoryMock.findByFid(fid)).thenReturn(Optional.ofNullable(organiseringState));
    }

    private static DvhOrganisering initialDvhOrganisering() {
        return new DvhOrganisering(
            "agressoIdOver",
            "orgNivOver",
            "navnOver",
            "agressoIdUnder",
            "orgNivUnder",
            "navnUnder",
            LocalDate.now().minusDays(1),
            null
        );
    }
}
