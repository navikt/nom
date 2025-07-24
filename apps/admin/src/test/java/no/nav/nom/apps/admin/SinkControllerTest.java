package no.nav.nom.apps.admin;

import io.getunleash.Unleash;
import no.nav.nom.apps.admin.dto.EventSearchParams;
import no.nav.nom.apps.admin.events.ressurs.eventsource.RessursEvtsrcRepository;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SinkControllerTest {

    @Mock
    private RessursEvtsrcRepository ressursEvtsrcRepository;

    @Mock
    private Unleash unleash;

    @InjectMocks
    private SinkController sinkController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testGetRessursEvents() {
        // Arrange
        EventSearchParams searchParams = new EventSearchParams(0, "A123456");
        RessursOpprettetEventV1 event1 = new RessursOpprettetEventV1(
            "fid1",
            RessursOpprettetEventV1.RessursOpprettetEventData.builder()
                .personident("12345698765")
                .sektor(Sektor.NAV_STATLIG)
                .visningsnavn(new Visningsnavn("Nav", Optional.empty(), "Navesen"))
                .primaryTelefonnummer("12345678")
                .fysiskeBrukersteder(List.of("brukersted1"))
                .primaryFysiskBrukersted("brukersted1")
                .identType(IdentType.STANDARD)
                .build(),
            new InputMetadata("REST-API", "createRessurs", null, null),
            RessursOpprettetEventV1.RessursOpprettetAdditionalData.builder()
                .navident("A123456")
                .kjoenn("M")
                .folkeregistrertNavn(new RessursOpprettetEventV1.RessursOpprettetAdditionalData.FolkeregistrertNavn("Nav", null, "Navesen"))
                .foedselsdato(LocalDate.of(1990, 1, 1))
                .startdato(LocalDate.of(2023, 1, 1))
                .orgEnhet("org1")
                .ansettelsesType(AnsettelsesType.FAST)
                .build()
        );

        RessursSektorOppdatertV1 event2 = new RessursSektorOppdatertV1(
            "fid1",
            new RessursSektorOppdatertV1.SektorOppdatertEventData(Sektor.NAV_STATLIG),
            new InputMetadata("REST-API", "updateSektor", null, null)
        );
        when(ressursEvtsrcRepository.getNavidentToFidMap()).thenReturn(Map.of("A123456", List.of("fid1")));
        when(ressursEvtsrcRepository.getRessursEvtsrc()).thenReturn(List.of(event1, event2));
        when(unleash.isEnabled(any())).thenReturn(true);

        // Act
        List<RessursEvent> result = sinkController.getRessursEvents(searchParams);

        // Assert
        assertEquals(2, result.size());
        assertEquals("fid1", result.get(0).getFid());
        assertEquals("fid1", result.get(1).getFid());
    }
}
