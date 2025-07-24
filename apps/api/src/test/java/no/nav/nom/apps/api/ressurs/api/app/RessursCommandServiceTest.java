package no.nav.nom.apps.api.ressurs.api.app;

import io.getunleash.Unleash;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventProducer;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RessursCommandServiceTest {

    @InjectMocks
    private RessursCommandService ressursCommandService;

    @Mock
    private RessursEventProducer eventProducer;

    @Mock
    private NomStateRessursListener nomStateRessursListener;

    @Mock
    private Unleash unleash;

    @Test
    @SneakyThrows
    void skal_opprette_ressurs_feiler_og_sletter_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = NavnDTO.builder().fornavn("test").mellomnavn("Yep").etternavn("tester").build();

        var request = ressursRequestBuilder(personident, navn);

        var metaData = metadataBuilder(request);

        var opprettRessursCommand = opprettRessursCommand(request, navident, metaData);

        CompletableFuture<RessursState> response = new CompletableFuture<>();
        CompletableFuture<SendResult<String, RessursEvent>> sendResultFuture = new CompletableFuture<>();

        when(nomStateRessursListener.waitForStateAfterEventCreation(anyString())).thenReturn(response);
        when(eventProducer.sendMessage(any())).thenAnswer(invocation -> CompletableFuture.supplyAsync(TimeoutException::new));
        when(eventProducer.sendMessage(any(RessursTekniskSlettetEventV1.class))).thenReturn(sendResultFuture);


        doNothing().when(nomStateRessursListener).removeWait(anyString());

        assertThrows(TimeoutException.class, () -> ressursCommandService.createRessursSync(opprettRessursCommand));

        verify(nomStateRessursListener).removeWait(anyString());
        verify(eventProducer).sendMessage(any(RessursTekniskSlettetEventV1.class));
    }

    private static OpprettRessursCommand opprettRessursCommand(OpprettRessursRequest request, String navident, InputMetadata metaData) {
        return new OpprettRessursCommand(
            request.personident(),
            navident,
            Person.builder()
                .fornavn(request.visningsfornavn())
                .mellomnavn(request.visningsmellomnavn())
                .etternavn(request.visningsetternavn())
                .kjonn(Kjonn.MANN)
                .foedselsdato(LocalDate.of(1980, 1, 1)).build(),
            Sektor.EKSTERN,
            new Visningsnavn(
                request.visningsfornavn(),
                Optional.of(request.visningsmellomnavn()),
                request.visningsetternavn()
            ),
            null,
            metaData,
            LocalDate.now().minusDays(1),
            "OrgEnhetId-1",
            AnsettelsesType.FAST,
            "2700",
            IdentType.STANDARD
        );
    }

    private static InputMetadata metadataBuilder(OpprettRessursRequest request) {
        return new InputMetadata(
            "REST-API",
            "opprettRessurs",
            request,
            new Origination(
                "admin",
                ""
            )
        );
    }

    private static OpprettRessursRequest ressursRequestBuilder(String personident, NavnDTO navn) {
        return OpprettRessursRequest.builder()
            .personident(personident)
            .visningsfornavn(navn.getFornavn())
            .visningsmellomnavn(navn.getMellomnavn())
            .visningsetternavn(navn.getEtternavn())
            .sektor(Sektor.EKSTERN)
            .orgEnhetId("OrgEnhetId-1")
            .startDato(LocalDate.now().minusDays(1))
            .sluttDato(LocalDate.now().plusYears(1))
            .ansettelsesType(AnsettelsesType.FAST)
            .fysiskBrukerstedNr("2700")
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();
    }
}
