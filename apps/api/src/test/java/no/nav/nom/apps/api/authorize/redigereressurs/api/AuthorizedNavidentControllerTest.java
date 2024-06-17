package no.nav.nom.apps.api.authorize.redigereressurs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class AuthorizedNavidentControllerTest extends RestTestBase {
    private static final String API_URL ="/app/authorizedRessursRedigering";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    AuthorizedNavident authorizedNavident;
    @MockBean
    AuthorizeCommandService authorizeCommandService;
    @MockBean
    DelegertLederStateRepository delegertLederStateRepository;

    @BeforeEach
    public void enableLederTilgangToggle() {
        when(unleash.isEnabled(AuthorizedNavidentController.UNLEASH_DELEGER_TILGANG)).thenReturn(true);
    }

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

//    @Test
//    @DisplayName("Avvise vanlig user token selv om leder er leder for aktuell ressurs for 'isAuthorizedForNavident'")
//    public void test_0003() throws Exception {
//        String navident = "B110001";
//        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + vanligUserToken_Leder_B()))
//            .andDo(print())
//            .andExpect(status().isForbidden());
//    }
//
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

//    @Test
//    @DisplayName("Avvise vanlig user token hvor selv om bruker er delegert leder for aktuell ressurs for 'isAuthorizedForNavident'")
//    public void test_0006() throws Exception {
//        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));
//
//        String navident = "D100001";
//        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + navident).header("Authorization", "Bearer " + vanligUserToken_DelegertLeder_HR()))
//            .andDo(print())
//            .andExpect(status().isForbidden());
//    }

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

//    @Test
//    @DisplayName("Avvise vanlig user token selv om leder er leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
//    public void test_0010() throws Exception {
//        String orgenhetId = "B11";
//        mockMvc.perform(get(API_URL + "/isAuthorizedForOrgenhet/" + orgenhetId).header("Authorization", "Bearer " + vanligUserToken_Leder_B()))
//            .andDo(print())
//            .andExpect(status().isForbidden());
//    }

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

//    @Test
//    @DisplayName("Avvise vanlig user token hvor selv om bruker er delegert leder for aktuell enhet for 'isAuthorizedForOrgenhet'")
//    public void test_0013() throws Exception {
//        when(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident("HR")).thenReturn(List.of("D"));
//
//        String orgenhetId = "D1";
//        mockMvc.perform(get(API_URL + "/isAuthorizedForNavident/" + orgenhetId).header("Authorization", "Bearer " + vanligUserToken_DelegertLeder_HR()))
//            .andDo(print())
//            .andExpect(status().isForbidden());
//    }

    @Test
    @DisplayName("Godkjenne NOM_ADMIN user token for 'isAuthorizedForOrgenhet'")
    public void test_0014() throws Exception {
        String orgenhetId = "D1";
        mockMvc.perform(get(API_URL + "/isAuthorizedForOrgenhet/" + orgenhetId).header("Authorization", "Bearer " + nomAdminUserToken()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verifiser at gyldigTom må være med i requesten")
    public void test_0015() throws Exception {
        var request = LederTilgangForOrgenhetRequest.builder().orgenhetId("a").delegertTilNavident("b").build();


        mockMvc.perform(post(API_URL + "/addDelegatedAuthorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + nomAdminUserToken()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verifiser at gyldigTom ikke kan være lenger bak i fortiden enn i går")
    public void test_0016() throws Exception {
        var request = LederTilgangForOrgenhetRequest.builder()
            .orgenhetId("B")
            .delegertTilNavident("HR")
            .gyldigTom(LocalDate.now().minusDays(2))
            .build();

        mockMvc.perform(post(API_URL + "/addDelegatedAuthorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + nomWriteUserToken_Leder_B()))
            .andExpect(status().isBadRequest());

        request = LederTilgangForOrgenhetRequest.builder()
            .orgenhetId("B")
            .delegertTilNavident("HR")
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();

        mockMvc.perform(post(API_URL + "/addDelegatedAuthorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + nomWriteUserToken_Leder_B()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verifiser gyldig request")
    public void test_0017() throws Exception {
        var request = LederTilgangForOrgenhetRequest.builder()
            .orgenhetId("B")
            .delegertTilNavident("HR")
            .gyldigTom(LocalDate.now().plusDays(1))
            .build();

        mockMvc.perform(post(API_URL + "/addDelegatedAuthorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + nomWriteUserToken_Leder_B()))
            .andExpect(status().isOk());
    }


    @Test
    @DisplayName("Verifiser at man ikke kan delegere til Eksterne")
    public void test_0018() throws Exception {
        var request = LederTilgangForOrgenhetRequest.builder().orgenhetId("B").delegertTilNavident("EKSTERN").gyldigTom(LocalDate.now().plusMonths(2)).build();

        mockMvc.perform(post(API_URL + "/addDelegatedAuthorization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + nomAdminUserToken()))
            .andExpect(status().isBadRequest());
    }
}
