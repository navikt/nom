package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
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
public class OrgEnhetSanityCheckServiceTest {

    private static final String ORGENHET_1_ID = "ab123c";
    private static final String ORGENHET_2_ID = "de456f";
    private static final String ORGENHET_3_ID = "gh789i";


    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepositoryMock;

    @Mock
    private OrganiseringStateRepository organiseringStateRepositoryMock;

    @Mock
    DelegertLederStateRepository delegertLederStateRepositoryMock;

    @Mock
    OrgEnhetLederStateRepository orgEnhetLederStateRepositoryMock;

    private OrgEnhetSanityCheckService service;

    @BeforeEach
    void setUp() {
        service = new OrgEnhetSanityCheckService(
            orgEnhetStateRepositoryMock, organiseringStateRepositoryMock,
            delegertLederStateRepositoryMock, orgEnhetLederStateRepositoryMock
        );
    }


    @Test
    void should_find_orgenheter_without_manager() {
        var orgenhetMedLeder = OrgEnhetState.builder()
            .id(ORGENHET_1_ID)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().plusYears(2))
            .uniqueAgressoId("052200-Gausdal-ORGNIV3")
            .build();
        var orgenhetUtenLeder = OrgEnhetState.builder()
            .id(ORGENHET_2_ID)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(LocalDate.now().plusYears(2))
            .uniqueAgressoId("089600-Telemark Karrieresenter-ORGNIV3")
            .build();

        when(orgEnhetStateRepositoryMock.getAll()).thenReturn(List.of(orgenhetMedLeder, orgenhetUtenLeder));

        // orgEnhetene er del av org-hierarkiet
        when(organiseringStateRepositoryMock.orgEnhetHasParent(ORGENHET_1_ID)).thenReturn(true);
        when(organiseringStateRepositoryMock.orgEnhetHasParent(ORGENHET_2_ID)).thenReturn(true);

        var orgEnhetLeder = OrgEnhetLederState.builder()
            .orgEnhetId(ORGENHET_1_ID)
            .build();
        when(orgEnhetLederStateRepositoryMock.getActiveForOrgEnhet(ORGENHET_1_ID)).thenReturn(Optional.of(orgEnhetLeder));
        when(orgEnhetLederStateRepositoryMock.getActiveForOrgEnhet(ORGENHET_2_ID)).thenReturn(Optional.empty());

        var orgenheterUtenLeder = service.listOrgenheterUtenLeder();

        assertThat(orgenheterUtenLeder.size()).isEqualTo(1);
        assertThat(orgenheterUtenLeder.getFirst().orgEnhetId()).isEqualTo(orgenhetUtenLeder.id());
    }

    @Test
    void should_not_include_orgenheter_without_manager_which_is_not_part_of_the_organization_hierarchy() {
        var orgenhetUtenLeder = OrgEnhetState.builder()
            .id(ORGENHET_2_ID)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(LocalDate.now().plusYears(2))
            .uniqueAgressoId("089600-Telemark Karrieresenter-ORGNIV3")
            .build();
        when(orgEnhetStateRepositoryMock.getAll()).thenReturn(List.of(orgenhetUtenLeder));

        // orgEnhet ikke en del av hierakiet
        when(organiseringStateRepositoryMock.orgEnhetHasParent(ORGENHET_2_ID)).thenReturn(false);


        var orgenheterUtenLeder = service.listOrgenheterUtenLeder();

        assertThat(orgenheterUtenLeder.size()).isEqualTo(0);
    }

    @Test
    void should_return_orgenheter_which_are_part_of_the_organization_and_thereby_active() {
        var activeOrgenhet1 = OrgEnhetState.builder()
            .id(ORGENHET_1_ID)
            .build();
        var activeOrgenhet2 = OrgEnhetState.builder()
            .id(ORGENHET_2_ID)
            .build();
        var inactiveOrgenhet = OrgEnhetState.builder()
            .id(ORGENHET_3_ID)
            .build();

        when(organiseringStateRepositoryMock.orgEnhetHasParent(ORGENHET_1_ID)).thenReturn(true);
        when(organiseringStateRepositoryMock.orgEnhetHasParent(ORGENHET_2_ID)).thenReturn(true);
        when(organiseringStateRepositoryMock.orgEnhetHasParent(ORGENHET_3_ID)).thenReturn(false);

        var aktiveOrgenheter = service.orgEnheterMedAktivOrganisering(List.of(activeOrgenhet1, activeOrgenhet2, inactiveOrgenhet));
        assertThat(aktiveOrgenheter.size()).isEqualTo(2);
    }

}
