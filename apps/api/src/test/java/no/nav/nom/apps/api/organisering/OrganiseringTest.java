package no.nav.nom.apps.api.organisering;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.val;
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
        val mockedOrganisering1 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("1111111").build();
        val mockedOrganisering2 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("2222222").build();
        val mockedOrganisering3 = DvhOrganiseringMock.getBuilderWithDefault().ORGNODE_KODE("3333333").build();

        dvhOrganiseringMockUtils.addToResponse(mockedOrganisering1, mockedOrganisering2, mockedOrganisering3);

        // When
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // Then
        verify(eventProducerMock, times(3)).sendMessage(eventCaptor.capture());

        List<OrganiseringOpprettetEventV1> events = eventCaptor.getAllValues();


    }
}
