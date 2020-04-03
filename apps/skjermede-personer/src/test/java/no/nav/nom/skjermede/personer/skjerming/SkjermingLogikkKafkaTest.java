package no.nav.nom.skjermede.personer.skjerming;

import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.contract.nom.dto.Ressurstype;
import no.nav.nom.skjermede.personer.SkjermedePersoner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SkjermedePersoner.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
public class SkjermingLogikkKafkaTest {

    @Autowired
    private SkjermetPersonRepository skjermetPersonRepository;

    @Autowired
    private SkjermingLogikkKafka skjermingLogikkKafka;

    @Value("${maanderPlusSkjerming}")
    private int mnderPlusSkjerming;

    private RessursDto ressursDto;

    private SkjermetPerson familieMedlem;

    @BeforeEach
    public void testDataSetup(){
        ressursDto = RessursDto.builder()
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
                .skjermetFom(ressursDto.getStartdato())
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
        Assertions.assertEquals(ressursDto.getStartdato(), savedSkjermetPerson.getSkjermetFom());
        Assertions.assertEquals(ressursDto.getFornavn(), savedSkjermetPerson.getFornavn());
        Assertions.assertEquals(ressursDto.getEtternavn(), savedSkjermetPerson.getEtternavn());
        Assertions.assertNull(savedSkjermetPerson.getTilknyttetRessurs());
        Assertions.assertNotNull(savedSkjermetPerson.getOpprettetDato());
        Assertions.assertNotNull(savedSkjermetPerson.getEndretDato());
    }

    @Test
    public void eksisterendeAnsattEndreInfo(){
        lagreSkjermetPerson(NomRessurserListener.converted(ressursDto));
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
        lagreSkjermetPerson(NomRessurserListener.converted(ressursDto));
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(LocalDate.now());
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming), savedSkjermetPerson.getSkjermetTom());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming), familie.getSkjermetTom());
    }

    @Test
    public void tidligereAnsattStarterIgjenInnenforSeksMnder(){
        LocalDate sluttdato = LocalDate.now();
        ressursDto.setSluttdato(sluttdato);
        familieMedlem.setSkjermetTom(sluttdato);
        lagreSkjermetPerson(NomRessurserListener.converted(ressursDto));
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setStartdato(sluttdato.plusMonths(5));
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato(), savedSkjermetPerson.getSkjermetTom());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato(), familie.getSkjermetTom());
    }

    @Test
    public void tidligereAnsattStarterIgjenUtenforSeksMnder(){
        LocalDate sluttdato = LocalDate.now().minusMonths(mnderPlusSkjerming).minusDays(1);
        ressursDto.setStartdato(sluttdato.minusMonths(1));
        ressursDto.setSluttdato(sluttdato);
        familieMedlem.setSkjermetFom(sluttdato.minusMonths(1));
        familieMedlem.setSkjermetTom(sluttdato);
        lagreSkjermetPerson(NomRessurserListener.converted(ressursDto));
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setStartdato(LocalDate.now());
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).get();
        Assertions.assertEquals(ressursDto.getStartdato(), savedSkjermetPerson.getSkjermetFom());
        Assertions.assertEquals(ressursDto.getSluttdato(), savedSkjermetPerson.getSkjermetTom());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familieMedlem.getSkjermetTom(), familie.getSkjermetTom());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermes(){
        SkjermetPerson skjermetAnsatt = NomRessurserListener.converted(ressursDto);
        skjermetAnsatt.setSkjermetTom(LocalDate.now().minusDays(1));
        lagreSkjermetPerson(skjermetAnsatt);

        lagreSkjermetPerson(familieMedlem);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson skjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(skjermetAnsatt.getSkjermetTom(), skjermetPerson.getSkjermetTom());
        Assertions.assertEquals(skjermetAnsatt.getSkjermetFom(), skjermetPerson.getSkjermetFom());

        Assertions.assertFalse(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).isPresent());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familieMedlem.getSkjermetTom(), familie.getSkjermetTom());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermesSlutter(){
        SkjermetPerson skjermetAnsatt = NomRessurserListener.converted(ressursDto);
        skjermetAnsatt.setSkjermetTom(LocalDate.now().minusDays(1));
        lagreSkjermetPerson(skjermetAnsatt);
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(LocalDate.now());
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson skjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(skjermetAnsatt.getSkjermetTom(), skjermetPerson.getSkjermetTom());
        Assertions.assertEquals(skjermetAnsatt.getSkjermetFom(), skjermetPerson.getSkjermetFom());

        Assertions.assertFalse(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).isPresent());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming), familie.getSkjermetTom());
    }

    private void lagreSkjermetPerson(SkjermetPerson skjermetPerson){
        skjermetPerson.setEndretDato(LocalDateTime.now());
        skjermetPerson.setOpprettetDato(LocalDateTime.now());
        skjermetPersonRepository.save(skjermetPerson);
    }
}