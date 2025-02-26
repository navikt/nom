package no.nav.nom.apps.api.ressurs;

import no.nav.nom.apps.api.common.SpringCacheConfig;
import no.nav.nom.apps.api.ressurs.api.app.NomRemedyIntegrasjonClient;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    NomRemedyIntegrasjonClient nomRemedyIntegrasjonClientMock;

    @BeforeEach
    public void setUp() {
        fysiskBrukerstedCache.emptyCache();
    }

    @Test
    public void testCacheHitForFysiskBrukersted() {
        when(nomRemedyIntegrasjonClientMock.getFysiskeBrukersteder()).thenReturn(List.of(new FysiskBrukersted("123", "Test")));

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        verify(nomRemedyIntegrasjonClientMock, times(1)).getFysiskeBrukersteder();
        verifyNoMoreInteractions(nomRemedyIntegrasjonClientMock);
    }

    @Test
    public void testEvictCacheForFysiskBrukersted() {
        when(nomRemedyIntegrasjonClientMock.getFysiskeBrukersteder()).thenReturn(List.of(new FysiskBrukersted("123", "Test")));

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        fysiskBrukerstedCache.emptyCache();

        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        verify(nomRemedyIntegrasjonClientMock, times(2)).getFysiskeBrukersteder();
        verifyNoMoreInteractions(nomRemedyIntegrasjonClientMock);
    }

}