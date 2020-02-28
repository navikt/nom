package no.nav.nom.skjermede.personer.skjerming;

import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class KafkaSkjermedePersonerListenerTest {

    @Mock
    private SkjermetPersonRepository skjermetPersonRepositoryMock;

    @InjectMocks
    private KafkaSkjermedePersonerListener kafkaSkjermedePersonerListener;

    @Captor
    private ArgumentCaptor<SkjermetPerson> skjermetPersonArgumentCaptor;


    @Test
    public void onMessageEksisterendePerson() {
        LocalDate startDato = LocalDate.parse("2020-01-01");
        LocalDate sluttDato = LocalDate.parse("2020-01-02");
        LocalDateTime opprettetDato = LocalDateTime.parse("2020-01-01T00:00:00");
        LocalDateTime oldEndretDato = LocalDateTime.parse("2020-01-01T00:00:00");
        long id = 1L;

        RessursDto ressursDto = RessursDto.builder()
                .personident("TestIdent")
                .ressurstype(Ressurstype.INTERN)
                .startdato(startDato)
                .sluttdato(sluttDato)
                .funksjonellId(1L)
                .build();
        SkjermetPerson savedSkjermetPerson = SkjermetPerson.builder()
                .id(id)
                .personident("TestIdent")
                .funksjonellId(1L)
                .skjermetFom(startDato)
                .opprettetDato(opprettetDato)
                .endretDato(oldEndretDato)
                .build();

        when(skjermetPersonRepositoryMock.findAllByPersonident(ressursDto.getPersonident())).thenReturn(Optional.of(savedSkjermetPerson));

        kafkaSkjermedePersonerListener.onMessage(ressursDto);

        verify(skjermetPersonRepositoryMock).save(savedSkjermetPerson);

        Assertions.assertEquals(ressursDto.getPersonident(), savedSkjermetPerson.getPersonident());
        Assertions.assertEquals(ressursDto.getStartdato(), savedSkjermetPerson.getSkjermetFom());
        Assertions.assertEquals(ressursDto.getSluttdato(), savedSkjermetPerson.getSkjermetTom());
        Assertions.assertEquals(ressursDto.getFunksjonellId(), savedSkjermetPerson.getFunksjonellId());

        Assertions.assertEquals(id, savedSkjermetPerson.getId());
        Assertions.assertEquals(opprettetDato, savedSkjermetPerson.getOpprettetDato());
        Assertions.assertNotEquals(oldEndretDato, savedSkjermetPerson.getEndretDato());

        Assertions.assertNull(savedSkjermetPerson.getTilknyttetRessurs());
    }

    @Test
    public void onMessageNyPerson(){
        RessursDto ressursDto = RessursDto.builder()
                .personident("TestIdent")
                .ressurstype(Ressurstype.INTERN)
                .startdato(LocalDate.now())
                .sluttdato(LocalDate.now())
                .funksjonellId(1L)
                .build();

        when(skjermetPersonRepositoryMock.findAllByPersonident(ressursDto.getPersonident())).thenReturn(Optional.empty());

        kafkaSkjermedePersonerListener.onMessage(ressursDto);
        verify(skjermetPersonRepositoryMock).save(skjermetPersonArgumentCaptor.capture());

        SkjermetPerson savedSkjemetPerson = skjermetPersonArgumentCaptor.getValue();

        Assertions.assertEquals(ressursDto.getPersonident(), savedSkjemetPerson.getPersonident());
        Assertions.assertEquals(ressursDto.getStartdato(), savedSkjemetPerson.getSkjermetFom());
        Assertions.assertEquals(ressursDto.getSluttdato(), savedSkjemetPerson.getSkjermetTom());
        Assertions.assertEquals(ressursDto.getFunksjonellId(), savedSkjemetPerson.getFunksjonellId());
        Assertions.assertNull(savedSkjemetPerson.getTilknyttetRessurs());
        Assertions.assertNotNull(savedSkjemetPerson.getOpprettetDato());
        Assertions.assertNotNull(savedSkjemetPerson.getEndretDato());
    }
}