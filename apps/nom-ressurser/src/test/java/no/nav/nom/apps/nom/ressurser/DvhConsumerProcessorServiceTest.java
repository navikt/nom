package no.nav.nom.apps.nom.ressurser;

import lombok.val;
import no.nav.nom.apps.nom.ressurser.dvh.DvhConsumerProcessorService;
import no.nav.nom.apps.nom.ressurser.dvh.DvhRessursDTO;
import no.nav.nom.apps.nom.ressurser.ressurser.Ressurs;
import no.nav.nom.apps.nom.ressurser.ressurser.RessursRepository;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DvhConsumerProcessorService.class)
public class DvhConsumerProcessorServiceTest {

    private static final LocalDate D2099 = LocalDate.of(2099, 12, 31);
    private static final String ANNEN_STATLIG = "Annen Statlig";
    private static final String KOMMUNALT = "NAV Kommunalt";
    private static final String EKSTERNT = "Eksternt";
    private static final String NAVIDENT = "A000000";
    private static final String FNR = "12345678910";

    private DvhRessursDTO dvhRessursDTO;

    @MockBean
    private RessursRepository ressursRepository;

    @Autowired
    private DvhConsumerProcessorService dvhConsumerProcessorService;

    @BeforeEach
    public void init() {
        dvhRessursDTO = DvhRessursDTO.builder()
                .navident(NAVIDENT)
                .personIdent(FNR)
                .remedySluttDato(LocalDate.now())
                .agressoSluttDato(LocalDate.now().minusDays(1))
                .ressursTypeKode("M")
                .ansattType("NAV Statlig")
                .permisjon("Nei")
                .build();
    }

    @Test
    public void testProcessRessurserStatligUtenRemedySluttdato() {
        dvhRessursDTO.setRemedySluttDato(D2099);
        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertEquals(arg.getValue().getSluttdato(), dvhRessursDTO.getAgressoSluttDato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.INTERN);
    }

    @Test
    public void testProcessRessurserStatligUtenRemedySluttdat() {
        dvhRessursDTO.setRemedySluttDato(D2099);
        dvhRessursDTO.setAgressoSluttDato(D2099);

        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());
        System.out.println(arg.getValue().getSluttdato());

        Assertions.assertNull(arg.getValue().getSluttdato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.INTERN);
    }

    @Test
    public void testProcessRessurserStatligMedRemedySluttdato() {
        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertEquals(arg.getValue().getSluttdato(), dvhRessursDTO.getAgressoSluttDato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.INTERN);
    }

    @Test
    public void testProcessRessurserStatligPermittert() {
        dvhRessursDTO.setPermisjon("JA");
        dvhRessursDTO.setAgressoSluttDato(null);

        when(ressursRepository.getGyldigRessursByNavident(anyString())).thenReturn(Optional.of(Ressurs.builder().sluttdato(null).build()));
        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertNull(arg.getValue().getSluttdato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.INTERN);
    }

    @Test
    public void testProcessRessurserMidlertidigEksternt() {
        dvhRessursDTO.setAnsattType(EKSTERNT);

        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertEquals(arg.getValue().getSluttdato(), dvhRessursDTO.getRemedySluttDato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.EKSTERN);
    }

    @Test
    public void testProcessRessurserEksternt() {
        dvhRessursDTO.setAnsattType(EKSTERNT);
        dvhRessursDTO.setRemedySluttDato(D2099);
        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertNull(arg.getValue().getSluttdato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.EKSTERN);
    }

    @Test
    public void testProcessRessurserKommunalt() {
        dvhRessursDTO.setAnsattType(KOMMUNALT);
        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertEquals(arg.getValue().getSluttdato(), dvhRessursDTO.getRemedySluttDato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.INTERN);
    }

    @Test
    public void testProcessRessurserAnnenStatlig() {
        dvhRessursDTO.setAnsattType(ANNEN_STATLIG);
        dvhConsumerProcessorService.processRessurser(Collections.singletonList(dvhRessursDTO));

        val arg = ArgumentCaptor.forClass(Ressurs.class);
        verify(ressursRepository).save(arg.capture());

        Assertions.assertEquals(arg.getValue().getSluttdato(), dvhRessursDTO.getRemedySluttDato());
        Assertions.assertEquals(arg.getValue().getRessurstype(), Ressurstype.ANNEN_STAT);
    }
}
