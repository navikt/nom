package no.nav.nom.apps.api.clients.msgraph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class MicrosoftGraphClientTest extends TestBase {

    private static final String ENTRA_ID_GROUP = "e152f131-2d26-4834-b6a0-30b2415f0385";
    private static final String ENTRA_ID_PATH = "/groups/" + ENTRA_ID_GROUP + "/members?$select=mail,onPremisesSamAccountName";
    private static final String EMAIL_PATH = "/users/ikke.svar.nom@nav.no/sendMail";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MicrosoftGraphClient microsoftGraphClient;


    @Test
    void should_follow_odata_nextLink_and_get_all_users_in_group() throws JsonProcessingException {
        ODataResponse<EntraIdUser> firstResponse = ODataResponse.<EntraIdUser>builder()
                .context("Some context")
                .count(2)
                .nextLink("/groups/nextPart")
                .value(List.of(user1()))
                .build();
        stubForGet(ENTRA_ID_PATH, firstResponse);

        ODataResponse<EntraIdUser> secondResponse = ODataResponse.<EntraIdUser>builder()
                .context("Some other context")
                .value(List.of(user2()))
                .build();
        stubForGet("/groups/nextPart", secondResponse);

        List<EntraIdUser> users = microsoftGraphClient.getUsersForGroup(ENTRA_ID_GROUP);
        assertThat(users.size()).isEqualTo(2);
        assertThat(users.getFirst()).isEqualTo(user1());
    }

    @Test
    void should_handle_failing_call_to_follow_odata_nextLink() throws JsonProcessingException {
        ODataResponse<EntraIdUser> firstResponse = ODataResponse.<EntraIdUser>builder()
                .context("Some context")
                .count(2)
                .nextLink("/groups/nextPart")
                .value(List.of(user1()))
                .build();
        stubForGet(ENTRA_ID_PATH, firstResponse);

        stubFor(get(urlEqualTo("/groups/nextPart"))
                .withHeader("authorization", matching("Bearer tokenValue"))
                .willReturn(aResponse().withStatus(400))
        );
        
        List<EntraIdUser> users = microsoftGraphClient.getUsersForGroup(ENTRA_ID_GROUP);
        assertThat(users.size()).isEqualTo(1);
    }

    @Test
    void should_handle_response_with_no_odata_nextLink_and_return_group_users() throws JsonProcessingException {
        ODataResponse<EntraIdUser> response = ODataResponse.<EntraIdUser>builder()
                .context("Some context")
                .count(2)
                .value(List.of(user1(), user2()))
                .build();
        stubForGet(ENTRA_ID_PATH, response);

        List<EntraIdUser> users = microsoftGraphClient.getUsersForGroup(ENTRA_ID_GROUP);
        assertThat(users.size()).isEqualTo(2);
    }

    @Test
    void should_handle_empty_odata_response() throws JsonProcessingException {
        ODataResponse<EntraIdUser> response = ODataResponse.<EntraIdUser>builder()
                .context("Some context")
                .value(Collections.emptyList())
                .build();
        stubForGet(ENTRA_ID_PATH, response);

        List<EntraIdUser> users = microsoftGraphClient.getUsersForGroup(ENTRA_ID_GROUP);
        assertThat(users.size()).isEqualTo(0);
    }

    private void stubForGet(String url, ODataResponse<EntraIdUser> response) throws JsonProcessingException {
        String responseBody = objectMapper.writeValueAsString(response);
        stubFor(get(urlEqualTo(url))
                .withHeader("authorization", matching("Bearer tokenValue"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        );
    }

    @Test
    void should_send_email() {
        stubFor(post(urlEqualTo(EMAIL_PATH))
                .withHeader("authorization", matching("Bearer tokenValue"))
                .willReturn(aResponse()
                        .withStatus(202)
                )
        );

        EmailMessage emailMessage = new EmailMessage("An Amazing message", "The message content", List.of("user1@nav.no"));
        microsoftGraphClient.sendEmail(emailMessage).block();

        verify(postRequestedFor(urlEqualTo(EMAIL_PATH))
                .withHeader("authorization", matching("Bearer tokenValue")));
    }

    private EntraIdUser user1() {
        return EntraIdUser.builder()
                .email("user1@nav.no")
                .navIdent("A123456")
                .build();
    }

    private EntraIdUser user2() {
        return EntraIdUser.builder()
                .email("user2@nav.no")
                .navIdent("B234567")
                .build();
    }
}