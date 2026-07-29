package no.nav.nom.apps.sfintegrasjon.sfclient.oauth;

import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SfTokenCacheTest {

    @Test
    void shouldFetchTokenOnFirstCall() {
        SfOAuthService mockService = mock(SfOAuthService.class);
        when(mockService.generateToken(any())).thenReturn(new SfAccessToken("token-1", "Bearer", 86400));

        SfTokenCache cache = new SfTokenCache(mockService);
        SfAccessToken token = cache.getToken();

        assertThat(token.accessToken()).isEqualTo("token-1");
        verify(mockService, times(1)).generateToken(any(SamlAssertionParams.class));
    }

    @Test
    void shouldReturnCachedTokenWithoutRefetching() {
        SfOAuthService mockService = mock(SfOAuthService.class);
        when(mockService.generateToken(any())).thenReturn(new SfAccessToken("token-1", "Bearer", 86400));

        SfTokenCache cache = new SfTokenCache(mockService);
        cache.getToken();
        cache.getToken();
        cache.getToken();

        verify(mockService, times(1)).generateToken(any());
    }

    @Test
    void shouldRefreshWhenTokenExpiresWithinTenMinutes() {
        SfOAuthService mockService = mock(SfOAuthService.class);
        when(mockService.generateToken(any()))
            .thenReturn(new SfAccessToken("token-1", "Bearer", 500))  // ~8 min — below threshold
            .thenReturn(new SfAccessToken("token-2", "Bearer", 86400));

        SfTokenCache cache = new SfTokenCache(mockService);
        SfAccessToken first = cache.getToken();   // fetches token-1 (500s ≈ 8 min < 10 min threshold, triggers immediate refresh)
        SfAccessToken second = cache.getToken();  // token-1 near expiry → fetches token-2

        assertThat(first.accessToken()).isEqualTo("token-1");
        assertThat(second.accessToken()).isEqualTo("token-2");
        verify(mockService, times(2)).generateToken(any());
    }

    @Test
    void shouldRefreshAfterInvalidate() {
        SfOAuthService mockService = mock(SfOAuthService.class);
        when(mockService.generateToken(any()))
            .thenReturn(new SfAccessToken("token-1", "Bearer", 86400))
            .thenReturn(new SfAccessToken("token-2", "Bearer", 86400));

        SfTokenCache cache = new SfTokenCache(mockService);
        cache.getToken();
        cache.invalidate();
        SfAccessToken second = cache.getToken();

        assertThat(second.accessToken()).isEqualTo("token-2");
        verify(mockService, times(2)).generateToken(any());
    }
}
