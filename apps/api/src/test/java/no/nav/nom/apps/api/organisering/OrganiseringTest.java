package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrganiseringTest extends TestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @MockBean
    OrganiseringEventCommandService organiseringEventCommandService;


    @Test
    @DisplayName("Innlesing av organiseringer")
    void innlesing_av_organiseringer() {
        // Given
        // Lage tre objekter i rest-responsen




        // When
        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        // Then
        verify(organiseringEventCommandService, times(3)).opprettOrganisering(any());

    }
}
