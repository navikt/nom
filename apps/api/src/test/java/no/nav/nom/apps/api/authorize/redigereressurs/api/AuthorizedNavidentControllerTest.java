package no.nav.nom.apps.api.authorize.redigereressurs.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.state.DelegertLederStateRepository;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class AuthorizedNavidentControllerTest extends RestTestBase {
    private static final String API_URL ="/api/app/authorizedRessursRedigering";

    @Autowired
    AuthorizedNavident authorizedNavident;
    @MockBean
    AuthorizeCommandService authorizeCommandService;
    @MockBean
    DelegertLederStateRepository delegertLederStateRepository;

    @Test
    @DisplayName("Godkjenne NOM-WRITE user token hvor leder er leder for aktuell ressurs for 'isAuthorizedForNavident'")
    public void test_0001() throws Exception {
        String navident = "B110001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + nomWriteUserToken_Leder_B()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Avvise NOM-WRITE user token hvor leder ikke er leder for aktuell ressurs for 'isAuthorizedForNavident'")
    public void test_0002() throws Exception {
        String navident = "B110001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + nomWriteUserToken_Leder_C()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Avvise vanlig user token selv om leder er leder for aktuell ressurs for 'isAuthorizedForNavident'")
    public void test_0003() throws Exception {
        String navident = "B110001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + vanligUserToken_Leder_B()))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Avvise invalid audiens token for user selv om leder er leder for aktuell ressurs for 'isAuthorizedForNavident'")
    public void test_0004() throws Exception {
        String navident = "B110001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + invalidAudienceToken()))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Godkjenne NOM-WRITE user token hvor bruker er delegert leder for aktuell ressurs for 'isAuthorizedForNavident'")
    public void test_0005() throws Exception {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        String navident = "D100001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + nomWriteUserToken_DelegertLeder_HR()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Avvise vanlig user token hvor selv om bruker er delegert leder for aktuell ressurs for 'isAuthorizedForNavident'")
    public void test_0006() throws Exception {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        String navident = "D100001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + vanligUserToken_DelegertLeder_HR()))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Godkjenne NOM_ADMIN user token for 'isAuthorizedForNavident'")
    public void test_0007() throws Exception {
        String navident = "D100001";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + nomAdminUserToken()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Godkjenne NOM-WRITE user token hvor leder er leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
    public void test_0008() throws Exception {
        String orgenhetId = "B11";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + nomWriteUserToken_Leder_B()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Avvise NOM-WRITE user token hvor leder ikke er leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
    public void test_0009() throws Exception {
        String orgenhetId = "B11";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + nomWriteUserToken_Leder_C()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Avvise vanlig user token selv om leder er leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
    public void test_0010() throws Exception {
        String orgenhetId = "B11";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + vanligUserToken_Leder_B()))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Avvise invalid audiens token for user selv om leder er leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
    public void test_0011() throws Exception {
        String orgenhetId = "B11";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + invalidAudienceToken()))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Godkjenne NOM-WRITE user token hvor bruker er delegert leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
    public void test_0012() throws Exception {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        String orgenhetId = "D1";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + nomWriteUserToken_DelegertLeder_HR()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Avvise vanlig user token hvor selv om bruker er delegert leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
    public void test_0013() throws Exception {
        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));

        String orgenhetId = "D1";
        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + vanligUserToken_DelegertLeder_HR()))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Godkjenne NOM_ADMIN user token for 'isAuthorizedForOrgenhet'")
    public void test_0014() throws Exception {
        String orgenhetId = "D1";
        mockMvc.perform(get(API_URL + "/isAuthorizedForOrgenhet/" + orgenhetId).header("Authorization", "Bearer " + nomAdminUserToken()))
            .andDo(print())
            .andExpect(status().isOk());
    }
}
