package no.nav.nom.apps.sfintegrasjon.outbox;

import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRestServiceTest {

    @Mock SfOutboxRepository sfOutboxRepository;
    @Mock SfOutboxService sfOutboxService;

    private OutboxRestService service;

    @BeforeEach
    void setUp() {
        service = new OutboxRestService(sfOutboxRepository, JsonMapper.builder().build(), sfOutboxService);
    }

    @Test
    void avvisteRaderFaarOrgenhetenSomJsonObjekt() {
        when(sfOutboxRepository.getAllBySfStatus(SfStatus.AVVIST)).thenReturn(List.of(SfOutbox.builder()
            .sporingsId(7L)
            .sfStatus(SfStatus.AVVIST)
            .feilmelding("400 fra SF")
            .orgEnhetId("ab1234")
            .endretAv(Set.of("Z123456"))
            .endringshendelser(Set.of("OrgEnhetEndretNavnEventV1"))
            .orgenhet("{\"id\":\"ab1234\",\"navn\":\"Enhet A\"}")
            .build()));

        assertThat(service.getAllRejected()).singleElement().satisfies(rad -> {
            assertThat(rad.sporingsId()).isEqualTo(7L);
            assertThat(rad.sfStatus()).isEqualTo(SfStatus.AVVIST);
            assertThat(rad.feilmelding()).isEqualTo("400 fra SF");
            assertThat(rad.orgEnhetId()).isEqualTo("ab1234");
            assertThat(rad.endretAv()).containsExactly("Z123456");
            assertThat(rad.endringshendelser()).containsExactly("OrgEnhetEndretNavnEventV1");
            assertThat(rad.orgenhet().isObject()).isTrue();
            assertThat(rad.orgenhet().get("navn").asString()).isEqualTo("Enhet A");
        });
    }

    @Test
    void radMedJsonSomIkkePasserSfOrgEnhetKanFortsattHentes() {
        when(sfOutboxRepository.getAllBySfStatus(SfStatus.AVVIST)).thenReturn(List.of(SfOutbox.builder()
            .sporingsId(8L)
            .sfStatus(SfStatus.AVVIST)
            .orgEnhetId("ab1234")
            .orgenhet("{\"id\":\"ab1234\",\"gyldigFom\":\"ikke-en-dato\"}")
            .build()));

        assertThat(service.getAllRejected()).singleElement()
            .satisfies(rad -> assertThat(rad.orgenhet().get("gyldigFom").asString()).isEqualTo("ikke-en-dato"));
    }

    @Test
    void feiledeRaderHentesMedStatusFeilet() {
        when(sfOutboxRepository.getAllBySfStatus(SfStatus.FEILET)).thenReturn(List.of());

        assertThat(service.getAllFailed()).isEmpty();
        verify(sfOutboxRepository).getAllBySfStatus(SfStatus.FEILET);
    }

    @Test
    void retrySenderRadeneTilEnhetenOgSvarerOk() {
        var rader = List.of(SfOutbox.builder().sporingsId(9L).sfStatus(SfStatus.AVVIST).orgEnhetId("ab1234").orgenhet("{}").build());
        when(sfOutboxRepository.getAllByOrgEnhetId("ab1234")).thenReturn(rader);

        var svar = service.retryBySporingsIds("ab1234");

        verify(sfOutboxService).sendPayloadsToCpi(rader);
        assertThat(svar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(svar.getBody().getMessage()).isEqualTo("OK");
    }

    @Test
    void retrySvarer500MedFeilmeldingenNaarSendingenFeiler() {
        when(sfOutboxRepository.getAllByOrgEnhetId("ab1234")).thenReturn(List.of());
        doThrow(new IllegalStateException("Noe gikk galt")).when(sfOutboxService).sendPayloadsToCpi(anyList());

        var svar = service.retryBySporingsIds("ab1234");

        assertThat(svar.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(svar.getBody().getMessage()).isEqualTo("Noe gikk galt");
    }
}
