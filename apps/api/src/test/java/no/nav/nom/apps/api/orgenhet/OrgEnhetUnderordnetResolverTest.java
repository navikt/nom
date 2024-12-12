package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.organiseringState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class OrgEnhetUnderordnetResolverTest extends OrgEnhetTestBase {

    @Autowired
    private OrgEnhetUnderordnetResolver orgEnhetUnderordnetResolver;

    @Test
    @DisplayName("Returner underordnede orgenheter, men ikke orgenheten selv")
    public void should_return_list_of_subordinate_orgenheter() {
        final String idOrgEnhet = "orgEnhet";
        final String idUnderordnet1 = "under1";
        final String idUnderordnet2 = "under2";
        final String idUnderordnet3 = "under3";

        OrganiseringState organiseringOrgEnhet = organiseringState().toBuilder()
            .id(idOrgEnhet)
            .idOver("someId")
            .build();
        OrganiseringState organiseringUnderordnet1 = organiseringState().toBuilder()
            .id(idUnderordnet1)
            .idOver(idOrgEnhet)
            .build();
        OrganiseringState organiseringUnderordnet2 = organiseringState().toBuilder()
            .id(idUnderordnet2)
            .idOver(idOrgEnhet)
            .build();
        OrganiseringState organiseringUnderordnet3 = organiseringState().toBuilder()
            .id(idUnderordnet3)
            .idOver(idOrgEnhet)
            .build();

        organiseringStateRepository.update(organiseringOrgEnhet);
        organiseringStateRepository.update(organiseringUnderordnet1);
        organiseringStateRepository.update(organiseringUnderordnet2);
        organiseringStateRepository.update(organiseringUnderordnet3);

        orgEnhetStateRepository.update(orgEnhetState().toBuilder().id(idUnderordnet1)
            .gyldigFom(LocalDate.now().minusDays(20))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(5)))
            .build());

        orgEnhetStateRepository.update(orgEnhetState().toBuilder().id(idUnderordnet2)
            .gyldigFom(LocalDate.now().minusDays(20))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(0)))
            .build());

        orgEnhetStateRepository.update(orgEnhetState().toBuilder().id(idUnderordnet3)
            .gyldigFom(LocalDate.now().minusDays(20))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(1)))
            .build());

        OrgEnhetState orgEnhet = orgEnhetState().toBuilder()
            .id(idOrgEnhet)
            .build();

        List<OrgEnhetState> underordnedeOrgEnheter = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhet);


        assertThat(underordnedeOrgEnheter.size()).isEqualTo(3);

        List<String> returnedOrgEnhetIds = underordnedeOrgEnheter.stream().map(OrgEnhetState::id).toList();
        assertThat(returnedOrgEnhetIds).contains(idUnderordnet1, idUnderordnet2, idUnderordnet3);
        assertThat(returnedOrgEnhetIds).doesNotContain(idOrgEnhet);
    }

    @Test
    @DisplayName("Ikke returner duplikate underordnede orgenheter")
    public void should_not_return_duplicate_subordinate_orgenheter() {
        final String idOrgEnhet = "orgEnhet";
        final String idUnderordnet = "under";

        OrganiseringState organiseringOrgEnhet = organiseringState().toBuilder()
            .id(idOrgEnhet)
            .idOver("someId")
            .build();
        // To underenheter med samme id, men forskjellig fid
        OrganiseringState organiseringUnderordnet1 = organiseringState().toBuilder()
            .id(idUnderordnet)
            .idOver(idOrgEnhet)
            .build();
        OrganiseringState organiseringUnderordnet2 = organiseringState().toBuilder()
            .id(idUnderordnet)
            .idOver(idOrgEnhet)
            .build();

        organiseringStateRepository.update(organiseringOrgEnhet);
        organiseringStateRepository.update(organiseringUnderordnet1);
        organiseringStateRepository.update(organiseringUnderordnet2);

        orgEnhetStateRepository.update(orgEnhetState().toBuilder().id(idUnderordnet)
            .gyldigFom(LocalDate.now().minusDays(20))
            .gyldigTom(Optional.of(LocalDate.now().plusDays(0)))
            .build());

        OrgEnhetState orgEnhet = orgEnhetState().toBuilder()
            .id(idOrgEnhet)
            .build();

        List<OrgEnhetState> underordnedeOrgEnheter = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhet);


        assertThat(underordnedeOrgEnheter.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ikke returner utgåtte orgenheter")
    public void should_not_return_expired_subordinate_orgenheter() {
        final String idOrgEnhet = "orgEnhet";
        final String idOrgEnhetOver = "orgEnhet_idOver";

        OrganiseringState organiseringOrgEnhet = organiseringState().toBuilder()
            .id(idOrgEnhet)
            .idOver(idOrgEnhetOver)
            .build();

        organiseringStateRepository.update(organiseringOrgEnhet);

        orgEnhetStateRepository.update(orgEnhetState().toBuilder()
            .id(idOrgEnhet)
            .gyldigFom(LocalDate.now().minusDays(20))
            .gyldigTom(Optional.of(LocalDate.now().minusDays(2)))
            .build());

        orgEnhetStateRepository.update(orgEnhetState().toBuilder().id(idOrgEnhetOver).build());

        OrgEnhetState orgEnhetOver = orgEnhetState().toBuilder().id(idOrgEnhetOver).build();
        List<OrgEnhetState> underordnedeOrgEnheter = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhetOver);

        assertThat(underordnedeOrgEnheter.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Ikke returner utgåtte orgenheter")
    public void should_not_return_subordinate_orgenheter_with_null_gyldigTOM() {
        final String idOrgEnhet = "orgEnhet";
        final String idOrgEnhetOver = "orgEnhet_idOver";

        OrganiseringState organiseringOrgEnhet = organiseringState().toBuilder()
            .id(idOrgEnhet)
            .idOver(idOrgEnhetOver)
            .build();

        organiseringStateRepository.update(organiseringOrgEnhet);

        orgEnhetStateRepository.update(orgEnhetState().toBuilder()
            .id(idOrgEnhet)
            .gyldigFom(LocalDate.now().minusDays(20))
            .build());

        orgEnhetStateRepository.update(orgEnhetState().toBuilder().id(idOrgEnhetOver).build());

        OrgEnhetState orgEnhetOver = orgEnhetState().toBuilder().id(idOrgEnhetOver).build();
        List<OrgEnhetState> underordnedeOrgEnheter = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhetOver);

        assertThat(underordnedeOrgEnheter.size()).isEqualTo(1); // når gyldigTom er null, orgenhetvurderes som aktiv
    }

}
