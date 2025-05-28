package no.nav.nom.apps.api.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;
import no.nav.nom.apps.api.common.security.AppGrantedAuthoritiesProperties;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static no.nav.nom.apps.api.TestTokensGenerator.LOGGED_IN_USER_NAVIDENT;
import static no.nav.nom.apps.api.common.TestData.orgEnhet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = { AuthResolver.class, TestTokensGenerator.class, JwtDecoder.class, AZPAuthoritiesConverter.class, AppGrantedAuthoritiesProperties.class })
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
public class AuthResolverTest {
    @MockitoBean
    AuthorizedNavident authorizedNavident;

    @Autowired
    AuthResolver authResolver;

    @Autowired
    TestTokensGenerator testToken;

    @Test
    @DisplayName("Test ADMIN kan redigere ressurs")
    void admin_kan_redigere_ressurs_test() {
        testToken.setNomAdminUserAuth();
        assertTrue(authResolver.kanRedigereRessurs("ANY"));
    }

    @Test
    @DisplayName("Test vanlig bruker ikke kan redigere ressurs")
    void vanlig_bruker_kan_ikke_redigere_ressurs_test() {
        testToken.setDefaultUserNomUiClient();
        assertFalse(authResolver.kanRedigereRessurs("ANY"));
    }

    @Test
    @DisplayName("Test leder bruker kan redigere ressurs i egen org")
    void leder_kan_redigere_ressurs_i_egen_org_test() {
        testToken.setNomWriteGroupAndNomUiAzpAuth();

        when(authorizedNavident.kanBrukerRedigereRessurs(LOGGED_IN_USER_NAVIDENT, "ANY")).thenReturn(true);

        assertTrue(authResolver.kanRedigereRessurs("ANY"));
    }

    @Test
    @DisplayName("Test leder kan redigere enhet")
    void leder_kan_redigere_enhet_test() {
        testToken.setNomWriteGroupAndNomUiAzpAuth();

        when(authorizedNavident.orgenheterLederKanRedigere(LOGGED_IN_USER_NAVIDENT)).thenReturn(List.of(orgEnhet("ORG1"), orgEnhet("ORG2")));

        assertTrue(authResolver.kanRessursRedigereOrgenhet(LOGGED_IN_USER_NAVIDENT, "ORG1"));
    }

    @Test
    @DisplayName("Test leder kan ikke redigere enhet")
    void leder_kan_ikke_redigere_enhet_test() {
        testToken.setNomWriteGroupAndNomUiAzpAuth();

        when(authorizedNavident.orgenheterLederKanRedigere(LOGGED_IN_USER_NAVIDENT)).thenReturn(List.of(orgEnhet("ORG1"), orgEnhet("ORG2")));

        assertFalse(authResolver.kanRessursRedigereOrgenhet(LOGGED_IN_USER_NAVIDENT, "ORG3"));
    }

    @Test
    @DisplayName("Test leder kan redigere minst en av enhetene")
    void leder_kan_redigere_minst_en_av_enhetene_test() {
        testToken.setNomWriteGroupAndNomUiAzpAuth();

        when(authorizedNavident.orgenheterLederKanRedigere(LOGGED_IN_USER_NAVIDENT)).thenReturn(List.of(orgEnhet("ORG1"), orgEnhet("ORG2")));

        assertTrue(authResolver.kanRedigereNoenAvOrgenhetene(Set.of("ORG1")));
    }

    @Test
    @DisplayName("Test leder kan ikke redigere minst en av enhetene")
    void leder_kan_ikke_redigere_minst_en_av_enhetene_test() {
        testToken.setNomWriteGroupAndNomUiAzpAuth();

        when(authorizedNavident.orgenheterLederKanRedigere(LOGGED_IN_USER_NAVIDENT)).thenReturn(List.of(orgEnhet("ORG1"), orgEnhet("ORG2")));

        assertFalse(authResolver.kanRedigereNoenAvOrgenhetene(Set.of("ORG3", "ORG4", "ORG5")));
    }
}
