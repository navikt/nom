package no.nav.nom.apps.sfintegrasjon.outbox;

import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfOutbox;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import no.nav.nom.apps.sfintegrasjon.outbox.repository.SfOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRestServiceTest {

    @Mock SfOutboxRepository sfOutboxRepository;

    private OutboxRestService service;

    @BeforeEach
    void setUp() {
        service = new OutboxRestService(sfOutboxRepository, JsonMapper.builder().build());
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
}
