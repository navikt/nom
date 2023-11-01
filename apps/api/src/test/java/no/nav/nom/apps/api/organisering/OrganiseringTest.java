package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventProducer;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrganiseringTest extends TestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    OrganiseringEventCommandService organiseringEventCommandService;

    @MockBean
    OrganiseringEventProducer eventProducerMock;

    @Captor
    ArgumentCaptor<OrganiseringOpprettetEventV1> eventCaptor;

    @Test
    @DisplayName("Innlesing av organiseringer")
    void innlesing_av_organiseringer() throws JsonProcessingException {

        // make mock response
        var mockedOrganisering1 = DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE("1111111")
            .ORGNODE_NAVN("Navn_1111111")
            .ORGNODE_KODE_OVER("01111111")
            .ORGNODE_NAVN_OVER("Navn_0111111")
            .ORGNODE_NIVA("Niva_111111")
            .ORGNODE_NIVA_OVER("Niva_0111111")
            .build();
        var mockedOrganisering2 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("2222222").build();
        var mockedOrganisering3 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("3333333").build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering1, mockedOrganisering2, mockedOrganisering3);

        // When
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // Then
        verify(eventProducerMock, times(3)).sendMessage(eventCaptor.capture());

        List<OrganiseringOpprettetEventV1> events = eventCaptor.getAllValues();

        var data = (OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData) events.get(0).getData();
        assertThat(data.agressoIdOver()).isEqualTo(mockedOrganisering1.getORGNODE_KODE_OVER());
        assertThat(data.agressoIdUnder()).isEqualTo(mockedOrganisering1.getORGNODE_KODE());
        assertThat(data.navnUnder()).isEqualTo(mockedOrganisering1.getORGNODE_NAVN());
        assertThat(data.orgNivOver()).isEqualTo(mockedOrganisering1.getORGNODE_NIVA_OVER());
        assertThat(data.orgNivUnder()).isEqualTo(mockedOrganisering1.getORGNODE_NIVA());
    }
}
