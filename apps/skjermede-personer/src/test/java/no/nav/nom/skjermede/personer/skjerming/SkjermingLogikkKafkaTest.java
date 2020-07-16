package no.nav.nom.skjermede.personer.skjerming;

import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.contract.nom.dto.Ressurstype;
import no.nav.nom.skjermede.personer.skjerming.kafka.listener.NomRessurserListener;
import no.nav.nom.skjermede.personer.skjerming.kafka.listener.SkjermingLogikkKafka;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application-test.yaml")
@ContextConfiguration(classes = SkjermingLogikkKafka.class)
public class SkjermingLogikkKafkaTest {

    @MockBean
    private SkjermetPersonRepository skjermetPersonRepository;

    @Value("${maanderPlusSkjerming}")
    private int mnderPlusSkjerming;

    @Autowired
    private SkjermingLogikkKafka skjermingLogikkKafka;

    private RessursDTO ressursDto;
    private SkjermetPerson skjermetPerson;
    private SkjermetPerson familieMedlem;

    @BeforeEach
    public void testDataSetup() {
        ressursDto = RessursDTO.builder()
                .personident("12345678910")
                .navident("T123456")
                .etternavn("etternavn")
                .fornavn("fornavn")
                .ressurstype(Ressurstype.INTERN)
                .startdato(LocalDate.now())
                .build();

        skjermetPerson = SkjermetPerson.builder()
                .personident("12345678910")
                .fornavn("fornavn")
                .etternavn("etternavn")
                .skjermetFra(LocalDateTime.now())
                .build();

        familieMedlem = SkjermetPerson.builder()
                .fornavn("Familie")
                .etternavn("Familiesen")
                .skjermetFra(ressursDto.getStartdato().atStartOfDay())
                .tilknyttetRessurs(ressursDto.getNavident())
                .personident("09876543210")
                .build();
    }

    @Test
    public void nyAnsatt() {
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.empty());
        when(skjermetPersonRepository.findAllByPersonident(anyString())).thenReturn(Collections.emptyList());

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository).save(arg.capture());
        val savedPerson = arg.getValue();

        Assertions.assertEquals(savedPerson.getOpprettetAv(), Kilde.SERVICEBRUKER);
        Assertions.assertNull(savedPerson.getTilknyttetRessurs());
        Assertions.assertNull(savedPerson.getSkjermetTil());
    }

    @Test
    public void nyEskternHendelse() {
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident())).thenReturn(Optional.empty());
        ressursDto.setRessurstype(Ressurstype.EKSTERN);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.EKSTERN);

        verify(skjermetPersonRepository, never()).save(any(SkjermetPerson.class));
    }

    @Test
    public void eksisterendeInternBlirEkstern() {
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident())).thenReturn(Optional.of(skjermetPerson));
        ressursDto.setRessurstype(Ressurstype.EKSTERN);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.EKSTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository).save(arg.capture());

        Assertions.assertNotNull(arg.getValue().getSkjermetTil());
    }

    @Test
    public void eksisterendeAnsattEndreInfo() {
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident())).thenReturn(Optional.of(skjermetPerson));

        ressursDto.setEtternavn("Nytt-Etternavn");
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository).save(arg.capture());

        Assertions.assertEquals(ressursDto.getEtternavn(), arg.getValue().getEtternavn());
    }

    @Test
    public void eksisterendeAnsattSlutter() {
        ressursDto.setSluttdato(LocalDate.now());
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(skjermetPerson.getPersonident())).thenReturn(Optional.of(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository).save(arg.capture());

        Assertions.assertNotNull(arg.getValue().getSkjermetTil());
        Assertions.assertTrue(arg.getValue().getSkjermetTil().isAfter(LocalDateTime.now().plusMonths(mnderPlusSkjerming)));
    }

    @Test
    public void tidligereAnsattStarterIgjenInnenforSeksMnder() {
        skjermetPerson.setSkjermetTil(LocalDateTime.now().plusMonths(4));
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(skjermetPerson.getPersonident())).thenReturn(Optional.of(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository).save(arg.capture());

        Assertions.assertNull(arg.getValue().getSkjermetTil());
    }

    @Test
    public void tidligereAnsattStarterIgjenUtenforSeksMnder() {
        skjermetPerson.setSkjermetTil(LocalDateTime.now().minusMonths(1));
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.empty());
        when(skjermetPersonRepository.findAllByPersonident(anyString())).thenReturn(Collections.singletonList(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository).save(arg.capture());

        Assertions.assertTrue(arg.getValue().getSkjermetFra().isAfter(skjermetPerson.getSkjermetTil()));
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermes() {
        skjermetPerson.setSkjermetTil(LocalDateTime.now().minusMonths(1));
        skjermetPerson.setEndretAv(Kilde.BRUKER);

        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.empty());
        when(skjermetPersonRepository.findAllByPersonident(anyString())).thenReturn(Collections.singletonList(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        verify(skjermetPersonRepository, never()).save(any());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermesSlutter() {
        skjermetPerson.setSkjermetTil(LocalDateTime.now().minusMonths(1));
        skjermetPerson.setEndretAv(Kilde.BRUKER);
        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.empty());
        when(skjermetPersonRepository.findAllByPersonident(anyString())).thenReturn(Collections.singletonList(skjermetPerson));

        ressursDto.setSluttdato(LocalDate.now());
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        verify(skjermetPersonRepository, never()).save(any());
    }

    @Test
    public void skjermetFamilieBlirAnsatt() {
        skjermetPerson.setTilknyttetRessurs("");

        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.of(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        val arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository, times(2)).save(arg.capture());
        val lagreKall = arg.getAllValues();

        Assertions.assertNotNull(lagreKall.get(0).getSkjermetTil());
        Assertions.assertNull(lagreKall.get(1).getTilknyttetRessurs());
        Assertions.assertTrue(lagreKall.get(1).getSkjermetFra().isAfter(lagreKall.get(0).getSkjermetTil()));
    }

    @Test
    public void tidligereSkjermetFamilieBlirAnsatt() {
        skjermetPerson.setSkjermetTil(LocalDateTime.now().minusMonths(1));
        skjermetPerson.setEndretAv(Kilde.BRUKER);
        skjermetPerson.setTilknyttetRessurs("");

        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.empty());
        when(skjermetPersonRepository.findAllByPersonident(anyString())).thenReturn(Collections.singletonList(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        verify(skjermetPersonRepository).save(any(SkjermetPerson.class));
    }

    @Test
    public void tidligereAnsattViderefortSkjermingEndrerSluttdatoTilForIdag() {
        skjermetPerson.setTilknyttetRessurs("");
        skjermetPerson.setSkjermetTil(LocalDateTime.now().minusMonths(2));
        ressursDto.setSluttdato(LocalDate.now().minusMonths(1));

        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.of(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        verify(skjermetPersonRepository, never()).save(any(SkjermetPerson.class));
    }

    @Test
    public void tidligereAnsattViderefortSkjermingEndrerSluttdatoTilEtterIdag() {
        skjermetPerson.setTilknyttetRessurs("");
        ressursDto.setSluttdato(LocalDate.now().plusMonths(2));

        when(skjermetPersonRepository.findAllByTilknyttetRessursAndValid(anyString())).thenReturn(Collections.emptyList());
        when(skjermetPersonRepository.getSkjermetPersonByPersonident(anyString())).thenReturn(Optional.of(skjermetPerson));

        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident(), Ressurstype.INTERN);

        ArgumentCaptor<SkjermetPerson> arg = ArgumentCaptor.forClass(SkjermetPerson.class);
        verify(skjermetPersonRepository, times(2)).save(arg.capture());
        val sistLagret = arg.getAllValues().get(1);

        Assertions.assertNull(sistLagret.getTilknyttetRessurs());
        Assertions.assertTrue(sistLagret.getSkjermetTil().toLocalDate().isAfter(ressursDto.getSluttdato()));
    }
}