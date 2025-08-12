package no.nav.nom.apps.navident.rnrservices;

import no.nav.nom.apps.navident.EmbeddedKafkaTestBase;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.idgenerator.NavidGenerator;
import no.nav.nom.apps.navident.pdl.PdlService;
import no.nav.nom.apps.navident.rnrservices.util.DomainTestData;
import no.nav.nom.contract.nom.IdentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.KNYTNING_FINNES_1_NavId;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.KNYTNING_FINNES_1_PersonId;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.KNYTNING_FINNES_FLERE_ID_3_NavId;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.KNYTNING_FINNES_FLERE_ID_3_PersonId_1;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.KNYTNING_FINNES_FLERE_ID_3_PersonId_2;
import static no.nav.nom.apps.navident.rnrservices.util.DomainTestData.KNYTNING_FINNES_IKKE_1_PersonId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class IdentServiceImplTest extends EmbeddedKafkaTestBase {

    @MockitoBean private PdlService pdlService;
    @MockitoBean private NavidGenerator navidGenerator;
    @Autowired private DomainTestData domainTestData;

    @Autowired private Map<String, Navident> navidenter;
    @Autowired private IdentService identService;


    @BeforeEach
    public void resetNavidenterDomainData() { domainTestData.prepNavidenter(navidenter); }

    @Test
    public void bestillIdent_knytningFinnesIkke() {
        String personid = KNYTNING_FINNES_IKKE_1_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.singletonList(personid));
        when(navidGenerator.generate(prefix)).thenReturn("A100001");

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus);
        assertEquals("A100001", respons.body.getNavident());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_knytningFinnes() {
        String personid = KNYTNING_FINNES_1_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.singletonList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);                        // gha HTTP 500 times - # 1
        assertEquals(KNYTNING_FINNES_1_NavId, respons.body.getNavident());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_knytningFinnes_FlerePersonideer_1() {
        String personid = KNYTNING_FINNES_FLERE_ID_3_PersonId_1;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.singletonList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_NavId, respons.body.getNavident());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_knytningFinnes_FlerePersonideer_2() {
        String personid = KNYTNING_FINNES_FLERE_ID_3_PersonId_2;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.singletonList(personid));

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(KNYTNING_FINNES_FLERE_ID_3_NavId, respons.body.getNavident());
        assertNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_KnytningFinnesIkke_FinnesIkkeIPdl() {
        String personid = "XXX";
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.emptyList());
        when(navidGenerator.generate('A')).thenReturn("A100001");

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.CREATED, respons.httpStatus); // gha HTTP 500 times - # 1
        assertEquals("A100001", respons.body.getNavident());
        assertNotNull(respons.body.getFeilmelding());
    }

    @Test
    public void bestillIdent_KnytningFinnes_FinnesIkkeIPdl() {
        String personid = KNYTNING_FINNES_1_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.emptyList());

        Respons respons = identService.bestillIdent(personid, IdentType.STANDARD, prefix);
        assertNotNull(respons);
        assertEquals(HttpStatus.OK, respons.httpStatus);
        assertEquals(KNYTNING_FINNES_1_NavId, respons.body.getNavident());
        assertNotNull(respons.body.getFeilmelding());
    }


    @Test
    public void bestillIdent_multiple_paralelle_kall() throws InterruptedException {
        String personid = KNYTNING_FINNES_IKKE_1_PersonId;
        char prefix = 'A';
        when(pdlService.getKnownPersonider(personid)).thenReturn(Collections.singletonList(personid));
        when(navidGenerator.generate(prefix)).thenReturn("A100001");

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        Callable<Respons> callableTask = () -> identService.bestillIdent(personid, IdentType.STANDARD, prefix);

        List<Future<Respons>> futures = executorService.invokeAll(Collections.nCopies(numberOfThreads, callableTask));

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        AtomicBoolean gotOkResponse = new AtomicBoolean(false);
        futures.forEach( future -> {
            try {
                Respons response = future.get();
                if (response.httpStatus == HttpStatus.CREATED) {
                    assertFalse(gotOkResponse.get(), "Flere kall på samme ident ble opprettet");
                    gotOkResponse.set(true);
                } else {
                    assertEquals(HttpStatus.CONFLICT, response.httpStatus, "Forventet HTTP 409 på resterende kall");
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}