package no.nav.nom.skjermede.personer.listener;

import no.nav.nom.contract.nom.dto.RessursDto;
import no.nav.nom.skjermede.personer.domain.SkjermetPerson;
import no.nav.nom.skjermede.personer.repository.SkjermetPersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class KafkaSkjermedePersonerListenerTest {

    @Mock
    private SkjermetPersonRepository skjermetPersonRepositoryMock;

    @Mock
    private RessursDto ressursDtoMock;

    @Mock
    private SkjermetPerson skjermetPersonMock;

    @InjectMocks
    private KafkaSkjermedePersonerListener kafkaSkjermedePersonerListener;


    @Test
    public void onMessageEksisterendePerson() {
        Optional<SkjermetPerson> skjermetPersonOptional = Optional.of(skjermetPersonMock);
        when(skjermetPersonRepositoryMock.findAllByPersonident(ressursDtoMock.getPersonident())).thenReturn(skjermetPersonOptional);

        kafkaSkjermedePersonerListener.onMessage(ressursDtoMock);
        verify(skjermetPersonRepositoryMock).save(skjermetPersonMock);
    }

    @Test
    public void onMessageNyPerson(){
        /*when(skjermetPersonRepositoryMock.findAllByPersonident(ressursDtoMock.getPersonident())).thenReturn(Optional.empty());
        //when(kafkaSkjermedePersonerListener.converted(any())).thenReturn(skjermetPersonMock);

        kafkaSkjermedePersonerListener.onMessage(ressursDtoMock);
        verify(skjermetPersonRepositoryMock).findAllByPersonident(ressursDtoMock.getPersonident());
        //verify(kafkaSkjermedePersonerListener.converted(ressursDtoMock));
        verify(skjermetPersonMock).setEndretDato(any());
        verify(skjermetPersonMock).setOpprettetDato(any());
        verify(skjermetPersonRepositoryMock).save(skjermetPersonMock);*/
    }

    @Test
    public void converted() {
        /*LocalDate startDato = LocalDate.parse("2020-01-01");
        LocalDate sluttDato = LocalDate.parse("2020-01-02");

        RessursDto ressursDto = RessursDto.builder()
                .funksjonellId(1L)
                .personident("TestIdent")
                .startdato(startDato)
                .build();

        SkjermetPerson skjermetPerson = KafkaSkjermedePersonerListener.converted(ressursDto);

        assert(skjermetPerson.getId() == null);
        assertEquals("TestIdent", skjermetPerson.getPersonident());
        assertEquals(1L, skjermetPerson.getFunksjonellId());
        assertEquals(startDato, skjermetPerson.getSkjermetFom());
        assert(skjermetPerson.getSkjermetTom() == null);
        assert(skjermetPerson.getTilknyttetRessurs() == null);
        assert(skjermetPerson.getOpprettetDato() == null);
        assert(skjermetPerson.getEndretDato() == null);

        ressursDto.setSluttdato(sluttDato);
        skjermetPerson = KafkaSkjermedePersonerListener.converted(ressursDto);
        assertEquals(sluttDato, skjermetPerson.getSkjermetTom());*/
    }
}