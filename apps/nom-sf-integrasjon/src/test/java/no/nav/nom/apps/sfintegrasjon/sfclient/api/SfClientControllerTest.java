package no.nav.nom.apps.sfintegrasjon.sfclient.api;

import no.nav.nom.apps.sfintegrasjon.config.SecurityConfig;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfODataClient;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfOAuthService;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.web.client.HttpClientErrorException;

@WebMvcTest(SfClientController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "NOM_ADMIN_GROUP_ID=test-admin-group",
    "AZURE_APP_TENANT_ID=test-tenant-id",
    "AZURE_APP_CLIENT_ID=test-client-id"
})
class SfClientControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    SamlAssertionService samlAssertionService;

    @MockitoBean
    SfOAuthService sfOAuthService;

    @MockitoBean
    SfODataClient sfODataClient;

    // --- /api/sf/debug/saml-assertion ---

    @Test
    void samlAssertion_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/sf/client/saml-assertion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void samlAssertion_shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/sf/client/saml-assertion")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void samlAssertion_shouldReturn200WithAssertionWhenAdmin() throws Exception {
        when(samlAssertionService.generateAssertion(any())).thenReturn("base64EncodedAssertion");

        mockMvc.perform(post("/api/sf/client/saml-assertion")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.assertion").value("base64EncodedAssertion"));
    }

    @Test
    void samlAssertion_shouldAcceptEmptyBody() throws Exception {
        when(samlAssertionService.generateAssertion(any())).thenReturn("assertion");

        mockMvc.perform(post("/api/sf/client/saml-assertion")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
            .andExpect(status().isOk());
    }

    // --- /api/sf/client/oauth-token ---

    @Test
    void oauthToken_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/sf/client/oauth-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void oauthToken_shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/sf/client/oauth-token")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void oauthToken_shouldReturn200WithTokenWhenAdmin() throws Exception {
        when(sfOAuthService.generateToken(any()))
            .thenReturn(new SfAccessToken("sf-access-token", "Bearer", 86400));

        mockMvc.perform(post("/api/sf/client/oauth-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("sf-access-token"))
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(86400));
    }

    // --- /api/sf/client/validate-token ---

    @Test
    void validateToken_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/sf/client/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"some-token\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/sf/client/validate-token")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"some-token\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void validateToken_shouldReturn200WithValidationResultWhenAdmin() throws Exception {
        String rawBody = "{\"access_token\":\"sf-access-token\",\"token_type\":\"Bearer\",\"expires_in\":86312}";
        when(sfOAuthService.validateToken(eq("sf-access-token"))).thenReturn(rawBody);

        mockMvc.perform(post("/api/sf/client/validate-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"sf-access-token\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json(rawBody));
    }

    @Test
    void validateToken_shouldReturn400WhenTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/sf/client/validate-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void validateToken_shouldReturn401WhenSapRejectsToken() throws Exception {
        when(sfOAuthService.validateToken(any()))
            .thenThrow(HttpClientErrorException.create(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized",
                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(post("/api/sf/client/validate-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid-token\"}"))
            .andExpect(status().isUnauthorized());
    }

    // --- /api/sf/client/odata-metadata/{entity} ---

    @Test
    void odataMetadata_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/sf/client/odata-metadata/User"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void odataMetadata_shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/sf/client/odata-metadata/User")
                .with(jwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    void odataMetadata_shouldReturn200WithXmlWhenAdmin() throws Exception {
        String xml = "<edmx:Edmx Version=\"1.0\"><edmx:DataServices><Schema Namespace=\"User\"/></edmx:DataServices></edmx:Edmx>";
        when(sfODataClient.getEntityMetadata(eq("User"))).thenReturn(xml);

        mockMvc.perform(get("/api/sf/client/odata-metadata/User")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(xml));
    }

    @Test
    void odataMetadata_shouldReturn400WhenEntityContainsNonLetters() throws Exception {
        mockMvc.perform(get("/api/sf/client/odata-metadata/User123")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void odataMetadata_shouldReturn400WhenEntityContainsSpecialChars() throws Exception {
        mockMvc.perform(get("/api/sf/client/odata-metadata/User%2FEntity")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void odataMetadata_shouldPropagateUpstreamErrorStatus() throws Exception {
        when(sfODataClient.getEntityMetadata(any()))
            .thenThrow(HttpClientErrorException.create(
                org.springframework.http.HttpStatus.NOT_FOUND, "Not Found",
                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(get("/api/sf/client/odata-metadata/Unknown")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
            .andExpect(status().isNotFound());
    }
}
