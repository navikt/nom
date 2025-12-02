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
    void should_return_orgenheter_without_manager() {
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

        var orgEnhetLeder = OrgEnhetLederState.builder()
            .orgEnhetId(ORGENHET_1_ID)
            .build();

        when(orgEnhetLederStateRepositoryMock.getActiveForOrgEnhet(ORGENHET_1_ID)).thenReturn(Optional.of(orgEnhetLeder));
        when(orgEnhetLederStateRepositoryMock.getActiveForOrgEnhet(ORGENHET_2_ID)).thenReturn(Optional.empty());

        var orgenheterUtenLeder = service.listOrgenheterUtenLeder();

        assertThat(orgenheterUtenLeder.size()).isEqualTo(1);
        assertThat(orgenheterUtenLeder.getFirst().orgEnhetId()).isEqualTo(orgenhetUtenLeder.id());
    }

}
