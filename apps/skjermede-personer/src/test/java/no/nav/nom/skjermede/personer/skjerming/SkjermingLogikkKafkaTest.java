package no.nav.nom.skjermede.personer.skjerming;

import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDTO;
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
        Assertions.assertEquals(ressursDto.getStartdato().atStartOfDay(), savedSkjermetPerson.getSkjermetFra());
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
        val sluttdato = LocalDate.now();
        ressursDto.setSluttdato(sluttdato);
        familieMedlem.setSkjermetTil(sluttdato.atTime(23, 59));
        lagreSkjermetPerson(NomRessurserListener.converted(ressursDto));
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setStartdato(sluttdato.plusMonths(5));
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato(), savedSkjermetPerson.getSkjermetTil());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato(), familie.getSkjermetTil());
    }

    @Test
    public void tidligereAnsattStarterIgjenUtenforSeksMnder(){
        val  sluttdato = LocalDate.now().minusMonths(mnderPlusSkjerming);

        ressursDto.setStartdato(sluttdato.minusMonths(1));
        ressursDto.setSluttdato(sluttdato);
        familieMedlem.setSkjermetFra(sluttdato.minusMonths(1).atStartOfDay());
        familieMedlem.setSkjermetTil(sluttdato.atTime(23, 59));
        lagreSkjermetPerson(NomRessurserListener.converted(ressursDto));
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setStartdato(sluttdato);
        ressursDto.setSluttdato(null);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(2, savedSkjermetPersonList.size());

        SkjermetPerson savedSkjermetPerson = skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).get();
        Assertions.assertEquals(ressursDto.getStartdato().atStartOfDay(), savedSkjermetPerson.getSkjermetFra());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familieMedlem.getSkjermetTil(), familie.getSkjermetTil());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermes(){
        SkjermetPerson skjermetAnsatt = NomRessurserListener.converted(ressursDto);
        skjermetAnsatt.setSkjermetTil(LocalDate.now().minusDays(1).atStartOfDay());
        lagreSkjermetPerson(skjermetAnsatt);

        lagreSkjermetPerson(familieMedlem);
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson skjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(skjermetAnsatt.getSkjermetTil(), skjermetPerson.getSkjermetTil());
        Assertions.assertEquals(skjermetAnsatt.getSkjermetFra(), skjermetPerson.getSkjermetFra());

        Assertions.assertFalse(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).isPresent());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(familieMedlem.getSkjermetTil(), familie.getSkjermetTil());
    }

    @Test
    public void eksisterendeAnsattVilIkkeSkjermesSlutter(){
        SkjermetPerson skjermetAnsatt = NomRessurserListener.converted(ressursDto);
        skjermetAnsatt.setSkjermetTil(LocalDate.now().minusDays(1).atTime(23, 59));
        lagreSkjermetPerson(skjermetAnsatt);
        lagreSkjermetPerson(familieMedlem);

        ressursDto.setSluttdato(LocalDate.now());
        skjermingLogikkKafka.handterAnsattHendelse(NomRessurserListener.converted(ressursDto), ressursDto.getNavident());

        List<SkjermetPerson> savedSkjermetPersonList = skjermetPersonRepository.findAllByPersonident(ressursDto.getPersonident());
        Assertions.assertEquals(1, savedSkjermetPersonList.size());

        SkjermetPerson skjermetPerson = savedSkjermetPersonList.get(0);
        Assertions.assertEquals(skjermetAnsatt.getSkjermetTil(), skjermetPerson.getSkjermetTil());
        Assertions.assertEquals(skjermetAnsatt.getSkjermetFra(), skjermetPerson.getSkjermetFra());

        Assertions.assertFalse(skjermetPersonRepository.getSkjermetPersonByPersonident(ressursDto.getPersonident()).isPresent());

        List<SkjermetPerson> familieList = skjermetPersonRepository.findAllByPersonident(familieMedlem.getPersonident());
        Assertions.assertEquals(1, familieList.size());

        SkjermetPerson familie = familieList.get(0);
        Assertions.assertEquals(ressursDto.getSluttdato().plusMonths(mnderPlusSkjerming).atTime(23, 59), familie.getSkjermetTil());
    }

    private void lagreSkjermetPerson(SkjermetPerson skjermetPerson){
        skjermetPerson.setEndretDato(LocalDateTime.now());
        skjermetPerson.setOpprettetDato(LocalDateTime.now());
        skjermetPerson.setOpprettetAv("test");
        skjermetPerson.setEndretAv("test");
        skjermetPersonRepository.save(skjermetPerson);
    }
}