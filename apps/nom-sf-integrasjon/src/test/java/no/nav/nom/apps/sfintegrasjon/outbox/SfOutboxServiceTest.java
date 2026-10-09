package no.nav.nom.apps.sfintegrasjon.outbox;

import io.getunleash.Unleash;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfCpiClient;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SfOutboxServiceTest {

    private static final String SVAR_FRA_SF = "{\"feil\":\"Ugyldig nivaa\"}";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String OUTBOX_TOGGLE = "nom-sf-integrasjon.outbox-service";

    @Mock SfOutboxRepository sfOutboxRepository;
    @Mock SfCpiClient sfCpiClient;
    @Mock LeaderElection leaderElection;
    @Mock Unleash unleash;

    @Captor ArgumentCaptor<Collection<SfStatus>> statusCaptor;
    @Captor ArgumentCaptor<SfOutboxDTO> sendtCaptor;
    @Captor ArgumentCaptor<SfOutbox> lagretCaptor;

    private SfOutboxService service;

    @BeforeEach
    void setUp() {
        lenient().when(unleash.isEnabled(OUTBOX_TOGGLE)).thenReturn(true);
        lenient().when(leaderElection.isLeader()).thenReturn(true);
        service = new SfOutboxService(sfOutboxRepository, sfCpiClient, leaderElection, JSON, unleash);
    }

    @Test
    @DisplayName("Gjør ingenting når togglen er av")
    void gjorIngentingNaarTogglenErAv() {
        when(unleash.isEnabled(OUTBOX_TOGGLE)).thenReturn(false);

        service.sendPayloadToSfCpi();

        verifyNoInteractions(sfOutboxRepository, sfCpiClient);
    }

    @Test
    @DisplayName("Gjør ingenting når poden ikke er leder")
    void gjorIngentingNaarPodenIkkeErLeder() {
        when(leaderElection.isLeader()).thenReturn(false);

        service.sendPayloadToSfCpi();

        verifyNoInteractions(sfOutboxRepository, sfCpiClient);
    }

    @Test
    @DisplayName("Henter rader med status IKKE_SENDT og FEILET, og sender ingenting når outboxen er tom")
    void henterRaderSomIkkeErSendtEllerHarFeilet() {
        outboxInneholder();

        service.sendPayloadToSfCpi();

        verify(sfOutboxRepository).getAllBySfStatusIn(statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(SfStatus.IKKE_SENDT, SfStatus.FEILET);
        verifyNoInteractions(sfCpiClient);
        verify(sfOutboxRepository, never()).save(any(SfOutbox.class));
    }

    @Nested
    @DisplayName("Hvilke rader som sendes")
    class HvilkeRaderSomSendes {

        @Test
        @DisplayName("Bare nyeste rad per orgenhet sendes, og eldre rader blir ERSTATTET")
        void bareNyesteRadSendesOgEldreRaderBlirErstattet() {
            // 9 og 10 sikrer at sporingsId sammenlignes som tall og ikke som tekst
            outboxInneholder(
                rad(9, "aa0001", SfStatus.FEILET),
                rad(10, "aa0001", SfStatus.IKKE_SENDT),
                rad(2, "aa0001", SfStatus.IKKE_SENDT)
            );
            sfSpeilerForesporselen();

            service.sendPayloadToSfCpi();

            assertThat(sendtTilSf()).singleElement().satisfies(sendt -> {
                assertThat(sendt.sporingsId()).isEqualTo("10");
                assertThat(sendt.sfOrgEnhet().navn()).isEqualTo("Versjon 10");
                assertThat(sendt.endretAv()).containsExactly("Z990010");
                assertThat(sendt.endringshendelser()).containsExactly("OrgEnhetKorrigertEventV1");
            });
            var lagret = lagretPerSporingsId();
            assertThat(lagret.get(2L).getSfStatus()).isEqualTo(SfStatus.ERSTATTET);
            assertThat(lagret.get(9L).getSfStatus()).isEqualTo(SfStatus.ERSTATTET);
            assertThat(lagret.get(10L).getSfStatus()).isEqualTo(SfStatus.SENDT);
        }

        @Test
        @DisplayName("Hver orgenhet sendes for seg")
        void hverOrgenhetSendesForSeg() {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT), rad(2, "bb0002", SfStatus.IKKE_SENDT));
            sfSpeilerForesporselen();

            service.sendPayloadToSfCpi();

            assertThat(sendtTilSf()).extracting(SfOutboxDTO::sporingsId).containsExactlyInAnyOrder("1", "2");
            verify(sfOutboxRepository, times(2)).save(any(SfOutbox.class));
            assertThat(lagretPerSporingsId().values()).extracting(SfOutbox::getSfStatus).containsOnly(SfStatus.SENDT);
        }

        @Test
        @DisplayName("En feil på én orgenhet stopper ikke sendingen av de andre")
        void feilPaaEnOrgenhetStopperIkkeDeAndre() {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT), rad(2, "bb0002", SfStatus.IKKE_SENDT));
            when(sfCpiClient.sendOrgenhetPayload(any(SfOutboxDTO.class))).thenAnswer(invocation -> {
                SfOutboxDTO dto = invocation.getArgument(0);
                if (dto.sporingsId().equals("1")) {
                    throw httpFeil(500);
                }
                return ResponseEntity.ok(dto);
            });

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId();
            assertThat(lagret.get(1L).getSfStatus()).isEqualTo(SfStatus.FEILET);
            assertThat(lagret.get(2L).getSfStatus()).isEqualTo(SfStatus.SENDT);
        }

        @Test
        @DisplayName("Eldre rader blir ERSTATTET også når sendingen av nyeste rad feiler")
        void eldreRaderBlirErstattetOgsaaNaarSendingenFeiler() {
            outboxInneholder(rad(1, "aa0001", SfStatus.FEILET), rad(2, "aa0001", SfStatus.IKKE_SENDT));
            sfKaster(httpFeil(503));

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId();
            assertThat(lagret.get(1L).getSfStatus()).isEqualTo(SfStatus.ERSTATTET);
            assertThat(lagret.get(2L).getSfStatus()).isEqualTo(SfStatus.FEILET);
        }
    }

    @Nested
    @DisplayName("Svar fra SF")
    class SvarFraSf {

        @Test
        @DisplayName("Speilet sporingsId gir SENDT og fjerner feilmeldingen fra forrige forsøk")
        void speiletSporingsIdGirSendt() {
            outboxInneholder(rad(1, "aa0001", SfStatus.FEILET).toBuilder().feilmelding("Timeout i forrige forsøk").build());
            sfSpeilerForesporselen();

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.SENDT);
            assertThat(lagret.getFeilmelding()).isNull();
        }

        @Test
        @DisplayName("Svar uten body gir AVVIST")
        void svarUtenBodyGirAvvist() {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT));
            when(sfCpiClient.sendOrgenhetPayload(any(SfOutboxDTO.class))).thenReturn(ResponseEntity.ok().build());

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.AVVIST);
            assertThat(lagret.getFeilmelding()).isNotBlank();
        }

        @Test
        @DisplayName("Svar med en annen sporingsId gir AVVIST")
        void annenSporingsIdGirAvvist() {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT));
            when(sfCpiClient.sendOrgenhetPayload(any(SfOutboxDTO.class))).thenAnswer(invocation -> {
                SfOutboxDTO dto = invocation.getArgument(0);
                return ResponseEntity.ok(dto.toBuilder().sporingsId("999").build());
            });

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.AVVIST);
            assertThat(lagret.getFeilmelding()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Feil ved sending")
    class FeilVedSending {

        @ParameterizedTest(name = "HTTP {0} gir FEILET, slik at raden sendes på nytt")
        @ValueSource(ints = {500, 502, 503, 504, 408, 429})
        void midlertidigeHttpFeilGirFeilet(int statuskode) {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT));
            sfKaster(httpFeil(statuskode));

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.FEILET);
            assertThat(lagret.getFeilmelding()).contains(String.valueOf(statuskode), SVAR_FRA_SF);
        }

        @ParameterizedTest(name = "HTTP {0} gir AVVIST, slik at raden ikke sendes på nytt")
        @ValueSource(ints = {400, 401, 403, 404, 409, 422})
        void permanenteHttpFeilGirAvvist(int statuskode) {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT));
            sfKaster(httpFeil(statuskode));

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.AVVIST);
            assertThat(lagret.getFeilmelding()).contains(String.valueOf(statuskode), SVAR_FRA_SF);
        }

        @Test
        @DisplayName("Nettverksfeil eller timeout gir FEILET")
        void nettverksfeilGirFeilet() {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT));
            sfKaster(new ResourceAccessException("I/O error on POST request: Connection reset"));

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.FEILET);
            assertThat(lagret.getFeilmelding()).contains("Connection reset");
        }

        @Test
        @DisplayName("Svar som ikke kan leses, gir FEILET")
        void svarSomIkkeKanLesesGirFeilet() {
            outboxInneholder(rad(1, "aa0001", SfStatus.IKKE_SENDT));
            sfKaster(new RestClientException("Could not extract response"));

            service.sendPayloadToSfCpi();

            var lagret = lagretPerSporingsId().get(1L);
            assertThat(lagret.getSfStatus()).isEqualTo(SfStatus.FEILET);
            assertThat(lagret.getFeilmelding()).contains("Could not extract response");
        }

        @Test
        @DisplayName("Orgenhet i outboxen som ikke kan leses, gir AVVIST med fast prefiks uten å sende, og stopper ikke de andre enhetene")
        void orgenhetSomIkkeKanLesesGirAvvist() {
            // Som når en enumverdi fjernes i en deploy mens raden venter på sending
            outboxInneholder(
                rad(1, "aa0001", SfStatus.IKKE_SENDT).toBuilder().orgenhet("{\"id\":\"aa0001\",\"nivaa\":\"FINNES_IKKE\"}").build(),
                rad(2, "bb0002", SfStatus.IKKE_SENDT)
            );
            sfSpeilerForesporselen();

            service.sendPayloadToSfCpi();

            assertThat(sendtTilSf()).extracting(SfOutboxDTO::sporingsId).containsExactly("2");
            var lagret = lagretPerSporingsId();
            assertThat(lagret.get(1L).getSfStatus()).isEqualTo(SfStatus.AVVIST);
            assertThat(lagret.get(1L).getFeilmelding()).startsWith("Kunne ikke lese orgenheten i outboxen for sporingsId 1");
            assertThat(lagret.get(2L).getSfStatus()).isEqualTo(SfStatus.SENDT);
        }
    }

    private static SfOutbox rad(long sporingsId, String orgEnhetId, SfStatus status) {
        return SfOutbox.builder()
            .sporingsId(sporingsId)
            .orgEnhetId(orgEnhetId)
            .sfStatus(status)
            .endretAv(Set.of("Z" + (990000 + sporingsId)))
            .endringshendelser(Set.of("OrgEnhetKorrigertEventV1"))
            .orgenhet(JSON.writeValueAsString(
                SfOrgEnhet.builder().id(orgEnhetId).navn("Versjon " + sporingsId).nivaa(NomNivaa.ARBEIDSOMRAADE).build()
            ))
            .build();
    }

    private static RestClientResponseException httpFeil(int statuskode) {
        var status = HttpStatusCode.valueOf(statuskode);
        var body = SVAR_FRA_SF.getBytes(StandardCharsets.UTF_8);
        return status.is5xxServerError()
            ? HttpServerErrorException.create(status, "", new HttpHeaders(), body, StandardCharsets.UTF_8)
            : HttpClientErrorException.create(status, "", new HttpHeaders(), body, StandardCharsets.UTF_8);
    }

    private void outboxInneholder(SfOutbox... rader) {
        when(sfOutboxRepository.getAllBySfStatusIn(anyCollection())).thenReturn(List.of(rader));
    }

    private void sfSpeilerForesporselen() {
        when(sfCpiClient.sendOrgenhetPayload(any(SfOutboxDTO.class))).thenAnswer(invocation -> ResponseEntity.ok(invocation.getArgument(0)));
    }

    private void sfKaster(RuntimeException feil) {
        when(sfCpiClient.sendOrgenhetPayload(any(SfOutboxDTO.class))).thenThrow(feil);
    }

    private List<SfOutboxDTO> sendtTilSf() {
        verify(sfCpiClient, atLeastOnce()).sendOrgenhetPayload(sendtCaptor.capture());
        return sendtCaptor.getAllValues();
    }

    private Map<Long, SfOutbox> lagretPerSporingsId() {
        verify(sfOutboxRepository, atLeastOnce()).save(lagretCaptor.capture());
        var sisteVersjon = new LinkedHashMap<Long, SfOutbox>();
        lagretCaptor.getAllValues().forEach(rad -> sisteVersjon.put(rad.getSporingsId(), rad));
        return sisteVersjon;
    }
}
