package no.nav.nom.apps.api.dvh.dvhdiff;

import no.nav.nom.apps.api.DvhRessursUtils;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@DisplayName("dvh-diffing")
public class RessursEndringFraDvhTest extends DvhTestBase {

    @SpyBean
    private KafkaTemplate<String, Event> eventProducerTemplate;

    @Test
    @DisplayName("Ressurser fra datavarehus trigger opprett event")
    public void test1() {
        final var response = List.of(
                new DvhRessursUtils.RessursMock(
                        "O123456",
                        "12345678901",
                        "Navn 1",
                        "ENavn 1",
                        "NAV Statlig"
                ),
                new DvhRessursUtils.RessursMock(
                        "O123457",
                        "12345678902",
                        "Navn 2",
                        "ENavn 2",
                        "NAV Statlig"
                )
        );
        dvhRessursUtils.addToResponse(response);

        ArgumentCaptor<RessursOpprettetEventV1> eventArgumentCaptor = ArgumentCaptor.forClass(RessursOpprettetEventV1.class);

        loadFromDvh();
        Mockito.verify(eventProducerTemplate, Mockito.times(response.size())).sendDefault(Mockito.notNull(), eventArgumentCaptor.capture());
        final var eventsByNavident = eventArgumentCaptor.getAllValues()
                .stream()
                .collect(Collectors.toMap(e -> ((RessursOpprettetEventV1.RessursRegisteredEventData) e.getData()).navident(), Function.identity()));

        response.forEach(r -> assertOpprettetEventFromDvhRessurs(eventsByNavident.get(r.NAV_IDENT()), r));
    }

    @Test
    @DisplayName("Prøver ikke opprette hvis eksisterende Ressurs")
    public void test2() {
        RessursState existingRessurs = createRessursHendelse("");

        dvhRessursUtils.addToResponse(
                new DvhRessursUtils.RessursMock(
                        existingRessurs.navident(),
                        existingRessurs.personident(),
                        existingRessurs.visningsnavn().fornavn(),
                        existingRessurs.visningsnavn().etternavn(),
                        "NAV Statlig"
                )
        );
        loadFromDvh();
        Mockito.verifyNoInteractions(eventProducerTemplate);
    }
/*

    @Test
    @DisplayName("Personidenter med flere navidenter, så velges den som er aktiv i remedy")
    public void test9(){
        stubRessurser("responses/dvh/dvhAnsatt3_sammePnrForskjelligNavident.json");
        loadFromDvh();

        assertThat(ressursService.findByNavIdent("B123456").isPresent()).isTrue();
    }*/

    // test helpers

    private void stubRessurser(String fileName) {
        stubFor(get("/proxy/dvh/json/Hr/NOM2/NOM_ANSATTE_BUCKETS_v1").willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .withBodyFile(fileName)
        ));
    }

    private int hashRessurs(IRessurs r) {
        return r.getId().hashCode() ^ r.getNavIdent().hashCode() ^ r.getPersonIdent().hashCode();
    }

    private Collection<Integer> hashedRessurser(Collection<IRessurs> ressurser) {
        return ressurser
                .stream().map(this::hashRessurs).collect(Collectors.toCollection(ArrayList::new));
    }

    private void assertOpprettetEventFromDvhRessurs(RessursOpprettetEventV1 ressursSluttetEvent, DvhRessursUtils.RessursMock ressursMock){

        Assertions.assertNotNull(ressursSluttetEvent.getFid());

        RessursOpprettetEventV1.RessursRegisteredEventData eventData = (RessursOpprettetEventV1.RessursRegisteredEventData) ressursSluttetEvent.getData();

        Assertions.assertEquals(ressursMock.NAV_IDENT(), eventData.navident());
        Assertions.assertEquals(ressursMock.FNR(), eventData.personident());
        Assertions.assertEquals(DvhRessurs.toSektor(ressursMock.ANSATT_TYPE()), eventData.sektor());
        Assertions.assertEquals(ressursMock.FORNAVN(), eventData.visningsnavn().fornavn());
        Assertions.assertEquals(ressursMock.ETTERNAVN(), eventData.visningsnavn().etternavn());
        Assertions.assertNull(eventData.visningsnavn().mellomnavn());
    }

}
