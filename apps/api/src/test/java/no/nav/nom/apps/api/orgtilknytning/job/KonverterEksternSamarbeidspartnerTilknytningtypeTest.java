package no.nav.nom.apps.api.orgtilknytning.job;

import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class KonverterEksternSamarbeidspartnerTilknytningtypeTest {
    private KonverterEksternSamarbeidspartnerTilknytningtype konverterEksternSamarbeidspartnerTilknytningtype;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @Mock
    private NomMastered nomMastered;

    @BeforeEach
    void setUp() {
        konverterEksternSamarbeidspartnerTilknytningtype = new KonverterEksternSamarbeidspartnerTilknytningtype(
            orgtilknytningStateRepository,
            orgtilknytningCommandService,
            nomMastered,
            "dummyHelfoId",
            "dummySkattId",
            "dummyRiksrevisjonId"
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("NAVident")).thenReturn("X123456");
        Authentication authentication = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void testKonverterTilknytningtyperTilEksterneOrgEnheter() {
        var orgtilknytninger = List.of(
            orgtilknytningState().toBuilder()
                .fid("fid-1")
                .navident("A123456")
                .orgEnhetId("dummyHelfoId")
                .orgtilknytningType(Optional.of(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER))
                .gyldigTom(Optional.of(LocalDate.now().minusWeeks(2)))
                .build(),
            orgtilknytningState().toBuilder()
                .fid("fid-2")
                .navident("B123456")
                .orgEnhetId("dummyHelfoId")
                .orgtilknytningType(Optional.of(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER))
                .gyldigTom(Optional.of(LocalDate.now().plusYears(1)))
                .build(),
            orgtilknytningState().toBuilder()
                .fid("fid-3")
                .navident("C123456")
                .orgEnhetId("dummySkattId")
                .orgtilknytningType(Optional.of(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER))
                .gyldigTom(Optional.of(LocalDate.now().plusYears(1)))
                .build(),
            orgtilknytningState().toBuilder()
                .fid("fid-4")
                .navident("D123456")
                .orgEnhetId("dummyRiksrevisjonId")
                .orgtilknytningType(Optional.of(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER))
                .build()
        );
        when(getAuthenticatedNavIdent()).thenReturn("X123456");
        when(orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("dummyHelfoId")).thenReturn(List.of(orgtilknytninger.get(0), orgtilknytninger.get(1)));
        when(orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("dummySkattId")).thenReturn(List.of(orgtilknytninger.get(2)));
        when(orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet("dummyRiksrevisjonId")).thenReturn(List.of(orgtilknytninger.get(3)));

        konverterEksternSamarbeidspartnerTilknytningtype.konverterTilknytningtyperTilEksterneOrgEnheter();

        verify(orgtilknytningCommandService, times(3)).endreTilknytningtype(any());

    }


}
