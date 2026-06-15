package no.nav.nom.apps.api.ressurs;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.SpringCacheConfig;
import no.nav.nom.apps.api.ressurs.api.app.MainManagerClient;
import no.nav.nom.apps.api.ressurs.api.app.NomRemedyIntegrasjonClient;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.net.URI;
import java.util.List;

import static no.nav.nom.apps.api.ressurs.FysiskBrukerstedCache.UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes ={
    FysiskBrukerstedCache.class,
    SpringCacheConfig.class
})
public class FysiskBrukerstedCacheTest {
    @Autowired
    FysiskBrukerstedCache fysiskBrukerstedCache;

    @MockitoBean
    MainManagerClient mainManagerClientMock;

    @MockitoBean
    NomRemedyIntegrasjonClient nomRemedyIntegrasjonClientMock;

    @MockitoBean
    Unleash unleashMock;

    @BeforeEach
    public void setUp() {
        when(unleashMock.isEnabled(UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED)).thenReturn(true);
        fysiskBrukerstedCache.emptyCache();
    }

    @Test
    public void testCacheHitForFysiskBrukersted() {
        when(mainManagerClientMock.getFysiskeBrukersteder()).thenReturn(List.of(
            FysiskBrukersted.builder().nr("123").navn("Test").build()));

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        verify(mainManagerClientMock, times(1)).getFysiskeBrukersteder();
        verifyNoMoreInteractions(mainManagerClientMock);
    }

    @Test
    public void testRefreshCacheReplacesDataWithoutEvicting() {
        when(mainManagerClientMock.getFysiskeBrukersteder()).thenReturn(List.of(FysiskBrukersted.builder().nr("123").navn("Test").build()));

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        fysiskBrukerstedCache.refreshCache();

        // Cache is now populated — getAll() should return cached data without another client call
        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        verify(mainManagerClientMock, times(2)).getFysiskeBrukersteder();
        verifyNoMoreInteractions(mainManagerClientMock);
    }

    @Test
    public void testRetryOnConnectionErrorDuringRefresh() {
        var connectionError = new WebClientRequestException(
            new RuntimeException("Connection refused"), HttpMethod.GET, URI.create("/api/v2/sites"), HttpHeaders.EMPTY);

        when(mainManagerClientMock.getFysiskeBrukersteder())
            .thenThrow(connectionError)
            .thenThrow(connectionError)
            .thenReturn(List.of(FysiskBrukersted.builder().nr("123").navn("Test").build()));

        fysiskBrukerstedCache.refreshCache();

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        verify(mainManagerClientMock, times(3)).getFysiskeBrukersteder();
        verifyNoMoreInteractions(mainManagerClientMock);
    }

    @Test
    public void testRefreshCacheKeepsExistingDataWhenAllRetriesFail() {
        when(mainManagerClientMock.getFysiskeBrukersteder())
            .thenReturn(List.of(FysiskBrukersted.builder().nr("123").navn("Test").build()));

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        clearInvocations(mainManagerClientMock);

        var connectionError = new WebClientRequestException(
            new RuntimeException("Connection refused"), HttpMethod.GET, URI.create("/api/v2/sites"), HttpHeaders.EMPTY);
        when(mainManagerClientMock.getFysiskeBrukersteder()).thenThrow(connectionError);

        fysiskBrukerstedCache.refreshCache();

        // Cache should still contain the old data
        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        // refreshCache() retried MAX_RETRY_ATTEMPTS times; getAll() above is a cache hit (0 additional calls)
        verify(mainManagerClientMock, times(FysiskBrukerstedCache.MAX_RETRY_ATTEMPTS)).getFysiskeBrukersteder();
    }

}