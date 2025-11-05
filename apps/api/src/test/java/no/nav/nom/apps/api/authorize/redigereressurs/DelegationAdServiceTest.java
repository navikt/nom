package no.nav.nom.apps.api.authorize.redigereressurs;

import no.nav.nom.apps.api.clients.msgraph.EntraIdUser;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DelegationAdService.class)
@TestPropertySource(properties = {
    "app.clients.microsoft-graph.groups.nom-delegert=42d11885-1e29-4fc4-9eed-1b339c2c8e36",
    "app.clients.microsoft-graph.groups.nom-leder=e9cd9267-9baf-4078-abb8-7251c4313d32",
    })
public class DelegationAdServiceTest {
    private final String AD_GROUP_NOM_WRITE_DELEGERT;
    private final String AD_GROUP_NOM_WRITE_LEDER;
    private static final String ENTRA_USER_1_NAVIDENT = "A123456";
    private static final String ENTRA_USER_2_NAVIDENT = "B234567";
    private static final String ENTRA_MANAGER_NAVIDENT = "C345678";


    public DelegationAdServiceTest(
        @Value("${app.clients.microsoft-graph.groups.nom-delegert}") String writeDelegertAdGroup,
        @Value("${app.clients.microsoft-graph.groups.nom-leder}") String writeLederAdGroup
    ) {
        this.AD_GROUP_NOM_WRITE_DELEGERT = writeDelegertAdGroup;
        this.AD_GROUP_NOM_WRITE_LEDER = writeLederAdGroup;
    }


    @Autowired
    DelegationAdService delegationAdService;

    @MockitoBean
    MicrosoftGraphClient microsoftGraphClient;

    @MockitoBean
    RessursStateRepository ressursStateRepository;


    @Test
    @DisplayName("Verifiserer at medarbeidere fra NOM_WRITE_DELEGERT og NOM_WRITE_LEDER hentes fra Azure AD og returneres samlet")
    void should_join_results_for_ad_groups() {
        when(microsoftGraphClient.getUsersForGroup(AD_GROUP_NOM_WRITE_DELEGERT)).thenReturn(List.of(user1(), user2()));
        when(microsoftGraphClient.getUsersForGroup(AD_GROUP_NOM_WRITE_LEDER)).thenReturn(List.of(manager()));

        when(ressursStateRepository.findByNavident(ENTRA_USER_1_NAVIDENT)).
            thenReturn(Optional.of(getRessursState("Ansatt1", "Ansatt")));
        when(ressursStateRepository.findByNavident(ENTRA_USER_2_NAVIDENT)).
            thenReturn(Optional.of(getRessursState("Ansatt1", "Ansatt")));
        when(ressursStateRepository.findByNavident(ENTRA_MANAGER_NAVIDENT)).
            thenReturn(Optional.of(getRessursState("Leder", "Leder")));

        List<MinimalRessurs> ressurser = delegationAdService.getRessurserWhichCanBeDelegatedTo();
        assertThat(ressurser).hasSize(3);
        assertThat(ressurser).anyMatch(ressurs -> ressurs.navident().equals(ENTRA_USER_1_NAVIDENT));
    }

    @Test
    @DisplayName("Verifiserer at exception i ett av kallene til Azure AD håndteres og det fortsatt returneres medarbeidere")
    void should_handle_exception_in_one_of_the_api_calls_when_getting_ad_groups() {
        when(microsoftGraphClient.getUsersForGroup(AD_GROUP_NOM_WRITE_DELEGERT)).thenReturn(List.of(user1(), user2()));
        when(microsoftGraphClient.getUsersForGroup(AD_GROUP_NOM_WRITE_LEDER))
            .thenThrow(new RuntimeException("Something went wrong"));

        when(ressursStateRepository.findByNavident(ENTRA_USER_1_NAVIDENT)).
            thenReturn(Optional.of(getRessursState("Ansatt1", "Ansatt")));
        when(ressursStateRepository.findByNavident(ENTRA_USER_2_NAVIDENT)).
            thenReturn(Optional.of(getRessursState("Ansatt1", "Ansatt")));

        List<MinimalRessurs> ressurser = delegationAdService.getRessurserWhichCanBeDelegatedTo();
        assertThat(ressurser).hasSize(2);
    }

    private RessursState getRessursState(String fornavn, String etternavn) {
        return RessursState.builder()
            .visningsnavn(new Visningsnavn(fornavn, Optional.empty(), etternavn))
            .build();
    }

    private EntraIdUser user1() {
        return EntraIdUser.builder()
            .email("user1@nav.no")
            .navIdent(ENTRA_USER_1_NAVIDENT)
            .build();
    }

    private EntraIdUser user2() {
        return EntraIdUser.builder()
            .email("user2@nav.no")
            .navIdent(ENTRA_USER_2_NAVIDENT)
            .build();
    }

    private EntraIdUser manager() {
        return EntraIdUser.builder()
            .email("manager@nav.no")
            .navIdent(ENTRA_MANAGER_NAVIDENT)
            .build();
    }
}
