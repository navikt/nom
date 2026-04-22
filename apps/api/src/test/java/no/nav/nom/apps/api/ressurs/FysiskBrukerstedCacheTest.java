package no.nav.nom.apps.api.ressurs;

import no.nav.nom.apps.api.common.SpringCacheConfig;
import no.nav.nom.apps.api.ressurs.api.app.MainManagerClient;
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
    MainManagerClient mainManagerClientMock;

    @BeforeEach
    public void setUp() {
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
    public void testEvictCacheForFysiskBrukersted() {
        when(mainManagerClientMock.getFysiskeBrukersteder()).thenReturn(List.of(FysiskBrukersted.builder().nr("123").navn("Test").build()));

        assertEquals(1, fysiskBrukerstedCache.getAll().size());
        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        fysiskBrukerstedCache.emptyCache();

        assertEquals(1, fysiskBrukerstedCache.getAll().size());

        verify(mainManagerClientMock, times(2)).getFysiskeBrukersteder();
        verifyNoMoreInteractions(mainManagerClientMock);
    }

}