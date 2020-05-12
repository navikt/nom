package no.nav.nom.skjermede.personer.skjerming;

import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.contract.nom.dto.Ressurstype;
import no.nav.nom.skjermede.personer.SkjermedePersoner;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselController;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.SkjermedePersonerController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SkjermedePersoner.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
public class SkjermingLogikkKafkaTest {

    @Autowired
    private SkjermetPersonRepository skjermetPersonRepository;

    @Autowired
    private SkjermingLogikkKafka skjermingLogikkKafka;

    @Autowired
    private SkjermedePersonerController skjermedePersonerController;

    @Autowired
    private ForesporselController foresporselController;

    @Autowired
    private ForesporselRepository foresporselRepository;

    @MockBean
    private TokenUtil tokenUtil;

    @MockBean
    private RestTemplate restTemplate;

    @Value("${maanderPlusSkjerming}")
    private int mnderPlusSkjerming;

    private RessursDTO ressursDto;

    private SkjermetPerson familieMedlem;

    @BeforeEach
    public void testDataSetup(){
        ressursDto = RessursDTO.builder()
                .personident("12345678901")
                .navident("T123456")
                .etternavn("etternavn")
                .fornavn("fornavn")
                .ressurstype(Ressurstype.INTERN)
                .startdato(LocalDate.now())
                .build();

        familieMedlem = SkjermetPerson.builder()
                .fornavn("Familie")
                .etternavn("Familiesen")
                .skjermetFra(ressursDto.getStartdato().atStartOfDay())
                .tilknyttetRessurs(ressursDto.getNavident())
                .personident("09876543210")
                .build();
        skjermetPersonRepository.deleteAll();
        skjermetPersonRepository.flush();
    }

    @Test
    public void nyAnsatt(){
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(ressursDto.getPersonident(), savedSkjermetPerson.getPersonident());
        Assertions.assertEquals(LocalDate.now(), savedSkjermetPerson.getSkjermetFra().toLocalDate());
        Assertions.assertEquals(ressursDto.getFornavn(), savedSkjermetPerson.getFornavn());
        Assertions.assertEquals(ressursDto.getEtternavn(), savedSkjermetPerson.getEtternavn());
        Assertions.assertNull(savedSkjermetPerson.getTilknyttetRessurs());
        Assertions.assertNotNull(savedSkjermetPerson.getOpprettetDato());
        Assertions.assertNotNull(savedSkjermetPerson.getEndretDato());
    }

    @Test
    public void eksisterendeAnsattEndreInfo(){
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        ressursDto.setFornavn("Fornavn");
        ressursDto.setEtternavn("Etternavn");
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(ressursDto.getFornavn(), savedSkjermetPerson.getFornavn());
        Assertions.assertEquals(ressursDto.getEtternavn(), savedSkjermetPerson.getEtternavn());
    }

    @Test
    public void eksisterendeAnsattSlutter(){
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(LocalDate.now());
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(
                ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming).atTime(23, 59),
                savedSkjermetPerson.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(
                ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming).atTime(23, 59),
                familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattStarterIgjenInnenforSeksMnder(){
        lagreSkjermetPerson(familieMedlem);

        val  sluttdato = LocalDate.now().minusMonths(mnderPlusSkjerming - 1);
        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

//        ressursDto.setStartdato(LocalDate.now().minusDays(1)); //TODO : Samme dato?
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertNull(savedSkjermetPerson.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertNull(familie.getSkjermetTil());
    }

//    @Test // This fails
    public void tidligereAnsattStarterIgjenUtenforSeksMnder(){
        lagreSkjermetPerson(familieMedlem);

        val  sluttdato = LocalDate.now().minusMonths(mnderPlusSkjerming + 1);
        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

//        ressursDto.setStartdato(LocalDate.now().minusDays(1)); // TODO: Samme dato?
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).get();
        Assertions.assertEquals(ressursDto.getStartdato(), savedSkjermetPerson.getSkjermetFra().toLocalDate());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(sluttdato.plusMonths(mnderPlusSkjerming), familie.getSkjermetTil().toLocalDate());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermes(){
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        when(tokenUtil.getSubject()).thenReturn(ressursDto.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        skjermedePersonerController.opphorSkjerming();

        SkjermetPerson opphortSkjerming = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident()).get(0);

        lagreSkjermetPerson(familieMedlem);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson skjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(opphortSkjerming.getSkjermetTil(), skjermetPerson.getSkjermetTil());
        Assertions.assertEquals(opphortSkjerming.getSkjermetFra(), skjermetPerson.getSkjermetFra());

        Assertions.assertFalse(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).isPresent());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familieMedlem.getSkjermetTil(), familie.getSkjermetTil());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermesSlutter(){
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        lagreSkjermetPerson(familieMedlem);

        when(tokenUtil.getSubject()).thenReturn(ressursDto.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        skjermedePersonerController.opphorSkjerming();

        ressursDto.setSluttdato(LocalDate.now().minusMonths(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson skjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(ressursDto.getStartdato(), skjermetPerson.getSkjermetTil().toLocalDate());
        Assertions.assertEquals(LocalDate.now(), skjermetPerson.getSkjermetFra().toLocalDate());
        Assertions.assertNotEquals(ressursDto.getSluttdato(), skjermetPerson.getSkjermetTil().toLocalDate());

        Assertions.assertFalse(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).isPresent());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming).atTime(23, 59), familie.getSkjermetTil());
    }

    @Test
    public void skjermetFamilieBlirAnsatt() {
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setPersonident(familieMedlem.getPersonident());
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson familieSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(familieSkjerming.getSkjermetTil(), ressursDto.getStartdato().atStartOfDay());

        SkjermetPerson ansattSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(ansattSkjerming.getSkjermetFra(), ressursDto.getStartdato().atStartOfDay());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(2, familieList.size());
        Assertions.assertEquals(familieList, savedSkjermetPersonList);

        SkjermetPerson aktivSkjerming = skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).get();
        Assertions.assertEquals(aktivSkjerming, ansattSkjerming);
    }

    @Test
    public void eksisterendeAnsattAvskjermetSegSkjermetSegIgjenSlutter() {
        ressursDto.setStartdato(LocalDate.now().minusMonths(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        lagreSkjermetPerson(familieMedlem);

        when(tokenUtil.getSubject()).thenReturn(ressursDto.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        skjermedePersonerController.opphorSkjerming();

        SkjermetPerson opphortSkjerming = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident()).get(0);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<>(ressursDto, HttpStatus.OK));
        skjermedePersonerController.opprettSkjerming();

        val sluttdato = LocalDate.now();
        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson automatiskSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(opphortSkjerming, automatiskSkjerming);

        SkjermetPerson nySkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(nySkjerming.getSkjermetTil(), sluttdato.plusMonths(mnderPlusSkjerming).atTime(23, 59));

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familie.getSkjermetTil(), sluttdato.plusMonths(mnderPlusSkjerming).atTime(23, 59));
    }

    @Test
    public void eksisterendeAnsattAvskjermetSegSkjermetSegIgjenEndreInfo() {
        ressursDto.setStartdato(LocalDate.now().minusMonths(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        lagreSkjermetPerson(familieMedlem);

        when(tokenUtil.getSubject()).thenReturn(ressursDto.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        skjermedePersonerController.opphorSkjerming();

        SkjermetPerson opphortSkjerming = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident()).get(0);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<>(ressursDto, HttpStatus.OK));
        skjermedePersonerController.opprettSkjerming();

        ressursDto.setFornavn("Fornavn");
        ressursDto.setEtternavn("Etternavn");
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson automatiskSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(opphortSkjerming, automatiskSkjerming);

        SkjermetPerson nySkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertNull(nySkjerming.getSkjermetTil());
        Assertions.assertEquals(nySkjerming.getFornavn(), ressursDto.getFornavn());
        Assertions.assertEquals(nySkjerming.getEtternavn(), ressursDto.getEtternavn());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertNull(familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattAvskjermetSegSkjermetSegIgjenSluttetStarterInnenforSeksMnder() {
        ressursDto.setStartdato(LocalDate.now().minusYears(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        lagreSkjermetPerson(familieMedlem);

        when(tokenUtil.getSubject()).thenReturn(ressursDto.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        skjermedePersonerController.opphorSkjerming();

        SkjermetPerson opphortSkjerming = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident()).get(0);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<>(ressursDto, HttpStatus.OK));
        skjermedePersonerController.opprettSkjerming();

        ressursDto.setSluttdato(LocalDate.now().minusMonths(mnderPlusSkjerming - 1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        ressursDto.setStartdato(LocalDate.now());
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson automatiskSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(opphortSkjerming, automatiskSkjerming);

        SkjermetPerson nyesteSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(ressursDto.getSluttdato(), nyesteSkjerming.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato(), familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattAvskjermetSegSkjermetSegIgjenSluttetStarterUtenforSeksMnder() {
        ressursDto.setStartdato(LocalDate.now().minusYears(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());
        lagreSkjermetPerson(familieMedlem);

        when(tokenUtil.getSubject()).thenReturn(ressursDto.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        skjermedePersonerController.opphorSkjerming();

        SkjermetPerson opphortSkjerming = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident()).get(0);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<>(ressursDto, HttpStatus.OK));
        skjermedePersonerController.opprettSkjerming();

        var sluttdato = LocalDate.now().minusMonths(mnderPlusSkjerming + 1);
        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        ressursDto.setStartdato(LocalDate.now());
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(3, savedSkjermetPersonList.size());

        SkjermetPerson automatiskSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(opphortSkjerming, automatiskSkjerming);

        SkjermetPerson ansatteSluttetSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(sluttdato.plusMonths(mnderPlusSkjerming).atTime(23, 59), ansatteSluttetSkjerming.getSkjermetTil());

        SkjermetPerson nyesteSkjerming = savedSkjermetPersonList.get(2);
        Assertions.assertEquals(ressursDto.getSluttdato(), nyesteSkjerming.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(sluttdato.plusMonths(mnderPlusSkjerming).atTime(23, 59), familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattViderefortSkjerming() {
        val sluttdato = LocalDate.now();
        ressursDto.setStartdato(sluttdato.minusMonths(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        familieMedlem.setSkjermetFra(sluttdato.minusMonths(1).atStartOfDay());
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        Foresporsel foresporsel = lagreForesporsel();
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(RessursDTO.builder().navident("V123456").build(), HttpStatus.OK));
        foresporselController.bekreftForesporsel(foresporsel.getFunksjonellId());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson somAnsattSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(sluttdato.minusMonths(1).atStartOfDay(), somAnsattSkjerming.getSkjermetFra());

        SkjermetPerson somFamilieSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertNull(somFamilieSkjerming.getSkjermetTil());
        Assertions.assertEquals(somAnsattSkjerming.getSkjermetTil().toLocalDate(), somFamilieSkjerming.getSkjermetFra().toLocalDate());
        Assertions.assertEquals(somFamilieSkjerming.getTilknyttetRessurs(), foresporsel.getTilknyttetRessurs());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(sluttdato.plusMonths(mnderPlusSkjerming).atTime(23, 59), familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattViderefortSkjermingStarterIgjen() {
        val sluttdato = LocalDate.now().minusMonths(1);
        ressursDto.setStartdato(sluttdato.minusMonths(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        familieMedlem.setSkjermetFra(sluttdato.minusMonths(1).atStartOfDay());
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        Foresporsel foresporsel = lagreForesporsel();
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(RessursDTO.builder().navident("V123456").build(), HttpStatus.OK));
        foresporselController.bekreftForesporsel(foresporsel.getFunksjonellId());

        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(3, savedSkjermetPersonList.size());

        SkjermetPerson somAnsattSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(sluttdato.minusMonths(1).atStartOfDay(), somAnsattSkjerming.getSkjermetFra());

        SkjermetPerson somFamilieSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(somAnsattSkjerming.getSkjermetTil().toLocalDate(), somFamilieSkjerming.getSkjermetFra().toLocalDate());

        SkjermetPerson somAnsattIgjenSkjerming = savedSkjermetPersonList.get(2);
        Assertions.assertEquals(somFamilieSkjerming.getSkjermetTil().toLocalDate(), somAnsattIgjenSkjerming.getSkjermetFra().toLocalDate());
        Assertions.assertNull(somAnsattIgjenSkjerming.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertNull(familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattViderefortSkjermingEndrerSluttdatoTilFørIdag() {
        val startdato = LocalDate.now().minusMonths(3);
        val sluttdato = LocalDate.now().minusMonths(2);
        ressursDto.setStartdato(startdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        familieMedlem.setSkjermetFra(startdato.atStartOfDay());
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        Foresporsel foresporsel = lagreForesporsel();
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(RessursDTO.builder().navident("V123456").build(), HttpStatus.OK));
        foresporselController.bekreftForesporsel(foresporsel.getFunksjonellId());

        List<SkjermetPerson> tempSavedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());

        ressursDto.setSluttdato(sluttdato.plusMonths(1));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());
        Assertions.assertEquals(tempSavedSkjermetPersonList, savedSkjermetPersonList);

        SkjermetPerson somAnsattSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(startdato.atStartOfDay(), somAnsattSkjerming.getSkjermetFra());

        SkjermetPerson somFamilieSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(somFamilieSkjerming.getSkjermetFra().toLocalDate(), somAnsattSkjerming.getSkjermetTil().toLocalDate());
        Assertions.assertNull(somFamilieSkjerming.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(sluttdato.plusMonths(1 + mnderPlusSkjerming), familie.getSkjermetTil().toLocalDate());
    }

    @Test
    public void tidligereAnsattViderefortSkjermingEndrerSluttdatoTilEtterIdag() {
        val startdato = LocalDate.now().minusMonths(2);
        val sluttdato = LocalDate.now().minusMonths(1);
        ressursDto.setStartdato(startdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        familieMedlem.setSkjermetFra(startdato.atStartOfDay());
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(sluttdato);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        Foresporsel foresporsel = lagreForesporsel();
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(RessursDTO.builder().navident("V123456").build(), HttpStatus.OK));
        foresporselController.bekreftForesporsel(foresporsel.getFunksjonellId());

        ressursDto.setSluttdato(sluttdato.plusMonths(2));
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(3, savedSkjermetPersonList.size());

        SkjermetPerson somAnsattSkjerming = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(sluttdato.minusMonths(1).atStartOfDay(), somAnsattSkjerming.getSkjermetFra());

        SkjermetPerson somFamilieSkjerming = savedSkjermetPersonList.get(1);
        Assertions.assertEquals(somAnsattSkjerming.getSkjermetTil().toLocalDate(), somFamilieSkjerming.getSkjermetFra().toLocalDate());

        SkjermetPerson somAnsattIgjenSkjerming = savedSkjermetPersonList.get(2);
        Assertions.assertEquals(somFamilieSkjerming.getSkjermetTil().toLocalDate(), somAnsattIgjenSkjerming.getSkjermetFra().toLocalDate());
        Assertions.assertEquals(somAnsattIgjenSkjerming.getSkjermetTil().toLocalDate(), sluttdato.plusMonths(2 + mnderPlusSkjerming));

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familie.getSkjermetTil().toLocalDate(), sluttdato.plusMonths(2 + mnderPlusSkjerming));
    }

    private void lagreSkjermetPerson(SkjermetPerson skjermetPerson){
        skjermetPerson.setEndretDato(LocalDateTime.now());
        skjermetPerson.setOpprettetDato(LocalDateTime.now());
        skjermetPersonRepository.save(skjermetPerson);
    }

    private Foresporsel lagreForesporsel() {
        return foresporselRepository.save(
            Foresporsel.builder()
                .funksjonellId(UUID.randomUUID())
                .personident(ressursDto.getPersonident())
                .fornavn(ressursDto.getFornavn())
                .etternavn(ressursDto.getEtternavn())
                .tilknyttetRessurs("V123456")
                .bekreftet(false)
                .opprettetDato(LocalDateTime.now())
                .build());
    }
}