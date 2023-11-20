package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService.OrganiseringIdTuple;
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
import java.util.List;
import java.util.Map;
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
        String nomId = "nomId";
        String nomIdOver = "nomIdOver";

        // Initial organisering from DVH
        DvhOrganisering initialDvhOrg = initialDvhOrganisering();

        // Setup response from organisering state lookup
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomId, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet orgEnhet = new OrgEnhet(OrgEnhetState.builder()
            .id(nomId)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // No change to OrgEnhet over
        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdOver)
            .navn(initialDvhOrg.getNavnOver())
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, initialDvhOrg, orgEnhet, orgEnhetOver)).isFalse();
    }

    @Test
    @DisplayName("Returnere true når navnOver er endret")
    void should_return_true_when_navnOver_has_changed() {
        String fid = "organisering-xyz123";
        String nomId = "nomId";
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
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomId, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet orgEnhet = new OrgEnhet(OrgEnhetState.builder()
            .id(nomId)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // OrgEnhet over has changed
        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(newNomIdOver)
            .navn(newNavnOver)
            .build());

        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, orgEnhet, orgEnhetOver)).isTrue();
    }

    @Test
    @DisplayName("Returnere true når orgNivOver er endret")
    void should_return_true_when_orgNivOver_has_changed() {
        String fid = "organisering-xyz123";
        String nomId = "nomId";
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
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomId, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet orgEnhet = new OrgEnhet(OrgEnhetState.builder()
            .id(nomId)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // OrgEnhet over has changed
        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(newNomIdOver)
            .navn(initialDvhOrg.getNavnOver())
            .agressoOrgNivaa(Optional.of(newOrgNivOver))
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, orgEnhet, orgEnhetOver)).isTrue();
    }

    @Test
    @DisplayName("Returnere true når agressoId er endret")
    void should_return_true_when_agressoId_has_changed() {
        String fid = "organisering-xyz123";
        String nomId = "nomId";
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
        mockOrganiseringStateLookup(fid, initialDvhOrg, nomId, nomIdOver);


        // Setup response from orgEnhet state lookup
        OrgEnhet orgEnhet = new OrgEnhet(OrgEnhetState.builder()
            .id(nomId)
            .navn(initialDvhOrg.getNavnUnder())
            .build());

        // OrgEnhet over has changed
        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(newNomIdOver)
            .navn(initialDvhOrg.getNavnOver())
            .agressoId(Optional.of(newAgressoIdOver))
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, orgEnhet, orgEnhetOver)).isTrue();
    }

    @Test
    @DisplayName("Returnere false når gyldigFom er endret")
    void should_return_false_if_gyldigFom_has_changed() {
        String fid = "organisering-xyz123";
        String nomId = "nomId";
        String nomIdOver = "nomIdOver";

        // Initial organisering from DVH
        DvhOrganisering initialDvhOrg = initialDvhOrganisering();

        // New organisering from DVH
        DvhOrganisering newDvhOrg = initialDvhOrganisering().toBuilder()
            .gyldigFom(LocalDate.now())
            .build();

        // Organisering state contains gyldigFom from when the organisering was created
        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid(fid)
            .id(nomId)
            .idOver(nomIdOver)
            .navn(initialDvhOrg.getNavnUnder())
            .navnOver(initialDvhOrg.getNavnOver())
            .gyldigTom(Optional.empty())
            .gyldigFom(LocalDate.now().minusDays(5))
            .build();

        when(organiseringStateRepositoryMock.findByFid(fid)).thenReturn(Optional.ofNullable(organiseringState));


        // Setup response from orgEnhet state lookup
        OrgEnhet orgEnhet = new OrgEnhet(OrgEnhetState.builder()
            .id(nomId)
            .build());

        OrgEnhet orgenhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(nomIdOver)
            .build());


        assertThat(organiseringService.hasOrganiseringChanged(fid, newDvhOrg, orgEnhet, orgenhetOver)).isFalse();
    }


    private void mockOrganiseringStateLookup(String fid, DvhOrganisering initialDvhOrg, String nomIdUnder, String nomIdOver) {
        OrganiseringState organiseringState = OrganiseringState.builder()
            .fid(fid)
            .id(nomIdUnder)
            .idOver(nomIdOver)
            .navn(initialDvhOrg.getNavnUnder())
            .navnOver(initialDvhOrg.getNavnOver())
            .gyldigTom(Optional.empty())
            .gyldigFom(LocalDate.now())
            .build();

        when(organiseringStateRepositoryMock.findByFid(fid)).thenReturn(Optional.ofNullable(organiseringState));
    }

    private static DvhOrganisering initialDvhOrganisering() {
        return new DvhOrganisering(
            "agressoIdOver",
            "orgNivOver",
            "navnOver",
            "agressoId",
            "orgNiv",
            "navn",
            LocalDate.now(),
            null
        );
    }

    @Test
    @DisplayName("Finne fids for organiseringer som er i state og ikke mottas fra DVH")
    void should_find_fids_in_state_but_not_in_dvh_organiseringer() {
        String fidNotFound = "fid_not_found";
        String fidFound = "fid_found";

        var organiseringerFromDvh = List.of(new OrganiseringIdTuple("id_found", "idOver_found"));

        var orgStateFound = OrganiseringState.builder()
            .fid(fidFound)
            .id("id_found")
            .idOver("idOver_found")
            .build();

        var orgStateNotFound = OrganiseringState.builder()
            .fid(fidNotFound)
            .id("id_not_found")
            .idOver("idOver_not_found")
            .build();

        var organiseringState = Map.of(fidFound, orgStateFound, fidNotFound, orgStateNotFound);

        var fidsNotFromDvh = organiseringService.fidsInStateNotInDvhOrganiseringer(organiseringerFromDvh, organiseringState);
        assertThat(fidsNotFromDvh.size()).isEqualTo(1);
        assertThat(fidsNotFromDvh.get(0)).isEqualTo(fidNotFound);
    }

}
